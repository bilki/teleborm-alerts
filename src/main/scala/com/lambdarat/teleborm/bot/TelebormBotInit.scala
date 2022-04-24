package com.lambdarat.teleborm.bot

import com.lambdarat.teleborm.domain.model.Messages
import com.lambdarat.teleborm.config.TelegramConfig

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all._
import com.bot4s.telegram.api.BotBase
import com.bot4s.telegram.marshalling._
import com.bot4s.telegram.methods.SetMyCommands
import com.bot4s.telegram.methods.SetWebhook
import com.bot4s.telegram.models.BotCommand
import com.bot4s.telegram.models.Update
import com.bot4s.telegram.models.User
import com.comcast.ip4s._
import com.scalawilliam.letsencrypt.LetsEncryptScala
import fs2.io.net.Network
import fs2.io.net.tls.TLSParameters
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server._
import org.http4s.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._

class TelebormBotInit[F[_]: Async: Logger](bot: BotBase[F], config: TelegramConfig) {

  private val dsl = new Http4sDsl[F] {}
  import dsl._

  private val botCommands =
    List(
      BotCommand(BormCommandType.Help.translation, Messages.helpCommandDescription),
      BotCommand(BormCommandType.Search.translation, Messages.searchCommandDescription),
      BotCommand("calendar", "Choose date")
      // BotCommand(BormCommandType.SearchWithDate.translation, Messages.searchFromCommandDescription)
    )

  val webhookHandler = HttpRoutes
    .of[F] { case req @ POST -> Root =>
      for {
        body     <- req.bodyText.compile.string
        update   <- Async[F].delay(fromJson[Update](body))
        _        <- bot.receiveUpdate(update, none[User])
        response <- Ok()
      } yield response
    }
    .orNotFound

  val webhookServer: EmberServerBuilder[F] = EmberServerBuilder
    .default[F]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8443")
    .withHttpApp(webhookHandler)

  val secureServer: Resource[F, EmberServerBuilder[F]] =
    for {
      letsEncrypt <- LetsEncryptScala.fromEnvironment[F]
      sslContext  <- letsEncrypt.sslContextResource[F]
      tlsContext = Network[F].tlsContext.fromSSLContext(sslContext)
    } yield webhookServer.withTLS(tlsContext, TLSParameters.Default)

  def setup(args: List[String]): F[Unit] = {
    val commandNames = botCommands.map(_.command).mkString("[", ", ", "]")
    val webhookUrl   = config.webhook.toString

    val registerWebhook = for {
      _            <- info"Attempting to register webhook handler at ${webhookUrl.toString}..."
      isRegistered <- bot.request(SetWebhook(url = webhookUrl.toString))
      _            <- info"Attempting to set commands ${commandNames}"
      commandsSet  <- bot.request(SetMyCommands(botCommands))
    } yield isRegistered && commandsSet

    val logRegisterSuccess =
      info"Registered webhook handler at ${webhookUrl.toString} with commands ${commandNames}"

    val server =
      if (args.contains("--insecure")) webhookServer.build
      else secureServer.flatMap(_.build)

    server.use { _ =>
      Async[F].ifM(registerWebhook)(
        logRegisterSuccess *> Async[F].never,
        Async[F].raiseError(new Exception(s"Could not set webhook URL to ${webhookUrl}"))
      )
    }
  }

}
