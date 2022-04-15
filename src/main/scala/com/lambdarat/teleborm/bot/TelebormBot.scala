package com.lambdarat.teleborm.bot

import com.lambdarat.teleborm.bot.Messages
import com.lambdarat.teleborm.bot.Messages._
import com.lambdarat.teleborm.handler.BormCommandHandler

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.Callbacks
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.marshalling._
import com.bot4s.telegram.methods.EditMessageText
import com.bot4s.telegram.methods.ParseMode
import com.bot4s.telegram.methods.SetMyCommands
import com.bot4s.telegram.methods.SetWebhook
import com.bot4s.telegram.models
import com.bot4s.telegram.models.BotCommand
import com.bot4s.telegram.models.Update
import com.bot4s.telegram.models.User
import com.comcast.ip4s._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._
import sttp.client3.SttpBackend

class TelebormBot[F[_]: Async: Logger](
    backend: SttpBackend[F, _],
    token: String,
    webhookUrl: Uri,
    handler: BormCommandHandler[F]
) extends TelegramBot[F](token, backend)
    with Commands[F]
    with Callbacks[F] {

  private val dsl = new Http4sDsl[F] {}
  import dsl._

  val webhookHandler = HttpRoutes
    .of[F] { case req @ POST -> Root =>
      for {
        body     <- req.bodyText.compile.string
        update   <- Async[F].delay(fromJson[Update](body))
        _        <- receiveUpdate(update, none[User])
        response <- Ok()
      } yield response
    }
    .orNotFound

  val webhookServer: Resource[F, Server] = EmberServerBuilder
    .default[F]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"9000")
    .withHttpApp(webhookHandler)
    .build

  private val botCommands =
    List(
      BotCommand(BormCommandType.Help.translation, Messages.helpCommandDescription),
      BotCommand(BormCommandType.Search.translation, Messages.searchCommandDescription),
      BotCommand(BormCommandType.SearchWithDate.translation, Messages.searchFromCommandDescription)
    )

  private implicit class RecoverFromCommand(commandAttempt: F[Unit])(implicit msg: models.Message) {
    def onErrorContact: F[Unit] =
      commandAttempt.recoverWith { case _ =>
        replyMdV2(
          Messages.contact.escape,
          disableWebPagePreview = true.some
        ).void
      }
  }

  // Greeting/help message
  onCommand("start" | BormCommandType.Help.translation) { implicit msg =>
    replyMdV2(Messages.greeting.escape).void
  }

  private val illegalCbData = new IllegalArgumentException(
    "Not supported command or data in callback"
  )

  onCallbackQuery { implicit cb =>
    val maybeCommand = BormCommand.extractFrom(cb.data)

    val attemptCommand = for {
      command <- Async[F].fromOption(maybeCommand, illegalCbData)
      _ <- command match {
        case search: BormCommand.Search =>
          for {
            searchResult <- handler.handleSearch(search)
            _ <- request(
              EditMessageText(
                chatId = cb.message.map(_.chat.chatId),
                messageId = cb.message.map(_.messageId),
                parseMode = ParseMode.MarkdownV2.some,
                disableWebPagePreview = true.some,
                text = searchResult.pretty.escape,
                replyMarkup = Pagination
                  .prepareSearchButtons(
                    search.words,
                    search.page.getOrElse(0),
                    searchResult.total
                  )
                  .some
              )
            )
          } yield ()
        case _ =>
          Async[F].raiseError(new IllegalArgumentException("Callback command not supported yet"))
      }
    } yield ()

    attemptCommand.handleErrorWith { case err =>
      error"Error while handling callback ${err.getMessage}" *> ackCallback().void
    }
  }

  // Search by words, optionally by date
  onCommand(BormCommandType.Search.translation) { implicit msg =>
    withArgs { args =>
      if (args.isEmpty) {
        reply(Messages.missingArgsForSearch).void
      } else {
        val attemptCommand = for {
          searchResult <- handler.handleSearch(
            BormCommand.Search(args.toList, none[Int], none[LocalDate])
          )
          _ <- replyMdV2(
            searchResult.pretty.escape,
            disableWebPagePreview = true.some,
            replyMarkup = Pagination.prepareSearchButtons(args.toList, 0, searchResult.total).some
          )
        } yield ()

        attemptCommand.onErrorContact
      }
    }
  }

  onCommand(BormCommandType.SearchWithDate.translation) { implicit msg =>
    withArgs {
      case Seq(rawDate, word, words @ _*) =>
        val dateOrError =
          Either.catchNonFatal(DateTimeFormatter.ISO_DATE.parse(rawDate, LocalDate.from _))

        dateOrError.fold(
          _ => reply(Messages.invalidDateForSearch(rawDate)).void,
          { _ =>
            val attemptCommand = for {
              searchResult <- handler.handleCommand(
                BormCommand.Search(word :: words.toList, none[Int], none[LocalDate])
              )
              _ <- replyMdV2(
                searchResult.escape,
                disableWebPagePreview = true.some
              )
            } yield ()

            attemptCommand.onErrorContact
          }
        )
      case _ => reply(Messages.missingArgsForSearchWithDate).void
    }
  }

  override def run(): F[Unit] = {
    val commandNames = botCommands.map(_.command).mkString("[", ", ", "]")

    val registerWebhook = for {
      _            <- info"Attempting to register webhook handler at ${webhookUrl.toString}..."
      isRegistered <- request(SetWebhook(url = webhookUrl.toString))
      _            <- info"Attempting to set commands ${commandNames}"
      commandsSet  <- request(SetMyCommands(botCommands))
    } yield isRegistered && commandsSet

    webhookServer.use { _ =>
      Async[F].ifM(registerWebhook)(
        info"Registered webhook handler at ${webhookUrl.toString} with commands ${commandNames}" *> Async[
          F
        ].never,
        Async[F].raiseError(new Exception(s"Could not set webhook URL to ${webhookUrl}"))
      )
    }
  }
}
