package com.lambdarat.teleborm

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import com.lambdarat.teleborm.bot.TelebormBot
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import pureconfig._
import pureconfig.module.catseffect.syntax._
import com.lambdarat.teleborm.config.TelegramConfig
import cats.effect.kernel.Sync
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.client3.logging.slf4j.Slf4jLoggingBackend

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

    AsyncHttpClientFs2Backend.resource[IO]().use { client =>
      for {
        config <- ConfigSource.default.at("telegram").loadF[IO, TelegramConfig]()
        _      <- unsafeLogger[IO].info("Loaded config, initializing bot...")
        bot = new TelebormBot[IO](Slf4jLoggingBackend(client), config.token, config.webhook)
        _ <- bot.run()
      } yield ExitCode.Success
    }
  }
}
