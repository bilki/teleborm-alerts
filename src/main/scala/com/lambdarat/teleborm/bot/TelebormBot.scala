package com.lambdarat.teleborm.bot

import cats.effect.kernel.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.Implicits._
import com.bot4s.telegram.marshalling._
import com.bot4s.telegram.methods.SetWebhook
import com.bot4s.telegram.models.Update
import com.bot4s.telegram.models.User
import com.comcast.ip4s._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server._
import org.http4s.implicits._
import sttp.client3.SttpBackend
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._
import cats.effect.kernel.Resource
import org.http4s.server.Server
import com.bot4s.telegram.methods.ParseMode

class TelebormBot[F[_]: Async: Logger](backend: SttpBackend[F, _], token: String, webhookUrl: Uri)
    extends TelegramBot[F](token, backend)
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
      """.stripMargin

    reply(escape(helpMessage), parseMode = ParseMode.MarkdownV2.some).void
  }

  override def run(): F[Unit] = {
    val registerWebhook = for {
      _            <- info"Attempting to register webhook handler at ${webhookUrl.toString}..."
      isRegistered <- request(SetWebhook(url = webhookUrl.toString))
    } yield isRegistered

    webhookServer.use { _ =>
      Async[F].ifM(registerWebhook)(
        info"Registered webhook handler at ${webhookUrl.toString}" *> Async[F].never,
        Async[F].raiseError(new Exception(s"Could not set webhook URL to ${webhookUrl}"))
      )
    }
  }
}
