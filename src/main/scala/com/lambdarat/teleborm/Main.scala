package com.lambdarat.teleborm

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import com.lambdarat.teleborm.bot.TelebormBot
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    AsyncHttpClientFs2Backend.resource[IO]().use { client =>
      val token      = "<add_your_token>"
      val webhookUrl = "<add_your_url>"
      val bot        = new TelebormBot[IO](client, token, webhookUrl)

      bot.run().as(ExitCode.Success)
    }
  }
}
