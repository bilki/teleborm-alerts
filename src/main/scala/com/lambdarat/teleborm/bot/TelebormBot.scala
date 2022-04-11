package com.lambdarat.teleborm.bot

import cats.effect.kernel.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.TelegramBot
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

class TelebormBot[F[_]: Async](backend: SttpBackend[F, _], token: String, webhookUrl: Uri)
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

  val webhookServer: F[Unit] = EmberServerBuilder
    .default[F]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"9000")
    .withHttpApp(webhookHandler)
    .build
    .use(_ => Async[F].never)

  override def run(): F[Unit] = {
    val setWebhookUrlRequest = request(SetWebhook(url = webhookUrl.toString))

    Async[F].ifM(setWebhookUrlRequest)(
      webhookServer,
      Async[F].raiseError(new Exception(s"Could not set webhook URL: ${webhookUrl}"))
    )
  }
}
