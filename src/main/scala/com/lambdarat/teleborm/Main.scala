package com.lambdarat.teleborm

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import com.lambdarat.teleborm.bot.TelebormBot
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import pureconfig._
import pureconfig.module.catseffect.syntax._
import com.lambdarat.teleborm.config.TelegramConfig

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    AsyncHttpClientFs2Backend.resource[IO]().use { client =>
      for {
        config <- ConfigSource.default.at("telegram").loadF[IO, TelegramConfig]()
        bot = new TelebormBot[IO](client, config.token, config.webhook)
        _ <- bot.run()
      } yield ExitCode.Success
    }
  }
}
