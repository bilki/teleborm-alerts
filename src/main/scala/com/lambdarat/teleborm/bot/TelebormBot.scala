package com.lambdarat.teleborm.bot

import com.lambdarat.teleborm.handler.BormCommandHandler

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all._
import com.bot4s.telegram.Implicits._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.marshalling._
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
    with Commands[F] {

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

  // Crude, brutal escape for markdown v2
  private def escape(text: String): String = text
    .replace(".", "\\.")
    .replace("|", "\\|")
    .replace("-", "\\-")

  private val botCommands =
    List(
      BotCommand("ayuda", "Recibe de nuevo el mensaje inicial de ayuda"),
      BotCommand(
        "buscar",
        "palabra1 palabra2 palabraN - Busca publicaciones que contengan todas las palabras"
      ),
      BotCommand(
        "buscar_desde",
        "2022-01-01 palabra1 palabra2 palabraN - Busca publicaciones que contengan todas las palabras desde la fecha indicada"
      )
    )

  private implicit class RecoverFromCommand(commandAttempt: F[Unit])(implicit msg: models.Message) {
    def onErrorContact: F[Unit] =
      commandAttempt.recoverWith { case _ =>
        reply(
          s"No se pudo completar la búsqueda, contacta con ${"\\@bilki".altWithUrl("https://twitter.com/bilki")} en Twitter para más información",
          parseMode = ParseMode.MarkdownV2.some,
          disableWebPagePreview = true.some
        ).void
      }
  }

  // Greeting/help message
  onCommand("start" | "ayuda") { implicit msg =>
    val helpMessage =
      s"""Bienvenido al buscador y notificador de publicaciones del ${"BORM".bold}.
      |
      |Este pequeño bot permite buscar por palabras clave, así como establecer
      |alertas para recibir mensajes con las nuevas publicaciones diarias.
      |
      |Puedes utilizar los siguientes comandos:
      |
      |/start o /ayuda - Recibe de nuevo este mensaje con la ayuda
      |
      |/buscar palabra1 palabra2 palabraN - Busca publicaciones que contengan ${"todas".bold} estas palabras
      |
      |/buscar\\_desde 2022-01-01 palabra1 palabra2 palabraN - Busca publicaciones que contengan ${"todas".bold} estas palabras desde la fecha indicada
      """.stripMargin

    reply(escape(helpMessage), parseMode = ParseMode.MarkdownV2.some).void
  }

  // Search by words, optionally by date
  onCommand("buscar") { implicit msg =>
    withArgs { args =>
      if (args.isEmpty) {
        reply("La búsqueda no funcionará si no se introduce al menos una palabra").void
      } else {
        val attemptCommand = for {
          searchResult <- handler.handleCommand(BormCommand.Search(args.toList, none[LocalDate]))
          _ <- reply(
            escape(searchResult),
            parseMode = ParseMode.MarkdownV2.some,
            disableWebPagePreview = true.some
          )
        } yield ()

        attemptCommand.onErrorContact
      }
    }
  }

  onCommand("buscar_desde") { implicit msg =>
    withArgs {
      case Seq(rawDate, word, words @ _*) =>
        val dateOrError =
          Either.catchNonFatal(DateTimeFormatter.ISO_DATE.parse(rawDate, LocalDate.from _))

        dateOrError.fold(
          _ => reply(s"La fecha proporcionada ${rawDate} no es válida").void,
          { from =>
            val attemptCommand = for {
              commandResult <- handler.handleCommand(
                BormCommand.Search(word :: words.toList, from.some)
              )
              _ <- reply(commandResult)
            } yield ()

            attemptCommand.onErrorContact
          }
        )
      case _ =>
        reply("La búsqueda no funcionará si no se introduce al menos la fecha y una palabra").void
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
