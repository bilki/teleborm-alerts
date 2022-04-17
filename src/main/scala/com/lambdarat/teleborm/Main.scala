package com.lambdarat.teleborm

import com.lambdarat.teleborm.bot.TelebormBot
import com.lambdarat.teleborm.bot.TelebormBotInit
import com.lambdarat.teleborm.client.BormClient
import com.lambdarat.teleborm.config.TelebormConfig
import com.lambdarat.teleborm.handler.BormCommandHandler

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Sync
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig._
import pureconfig.module.catseffect.syntax._
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.logging.slf4j.Slf4jLoggingBackend

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

    AsyncHttpClientFs2Backend.resource[IO]().use { client =>
      val loggingSttpClient = Slf4jLoggingBackend(client)

      for {
        config <- ConfigSource.default.loadF[IO, TelebormConfig]()
        _      <- unsafeLogger[IO].info("Loaded config, initializing bot...")
        bormClient     = new BormClient[IO](loggingSttpClient, config.borm)
        commandHandler = new BormCommandHandler[IO](bormClient, config.borm)
        bot = new TelebormBot[IO](
          loggingSttpClient,
          config.telegram.token,
          commandHandler
        )
        botInitializer = new TelebormBotInit[IO](bot, config.telegram)
        _ <- botInitializer.setup(args)
      } yield ExitCode.Success
    }
  }
}
