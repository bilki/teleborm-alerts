package com.lambdarat.teleborm

import com.lambdarat.teleborm.bot.TelebormBot
import com.lambdarat.teleborm.bot.TelebormBotInit
import com.lambdarat.teleborm.client.BormClient
import com.lambdarat.teleborm.config.TelebormConfig
import com.lambdarat.teleborm.database.DataSourceHelpers
import com.lambdarat.teleborm.database.FlywayLoader
import com.lambdarat.teleborm.database.UserStateStorage
import com.lambdarat.teleborm.handler.BormCommandHandler

import scala.concurrent.ExecutionContext

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import doobie._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig._
import pureconfig.module.catseffect.syntax._
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.logging.LogLevel
import sttp.client3.logging.slf4j.Slf4jLoggingBackend

object Main extends IOApp {
  implicit val ec = ExecutionContext.global

  def run(args: List[String]): IO[ExitCode] = {
    val program = for {
      implicit0(logger: Logger[IO]) <- Resource.eval(Slf4jLogger.create[IO])
      _      <- Resource.eval(logger.info("Created logger, attempting to create client..."))
      client <- AsyncHttpClientFs2Backend.resource[IO]()
      loggingSttpClient = Slf4jLoggingBackend(
        client,
        logRequestHeaders = false,
        logResponseHeaders = false,
        beforeRequestSendLogLevel = LogLevel.Trace
      )
      config <- Resource.eval(ConfigSource.default.loadF[IO, TelebormConfig]())
      _      <- Resource.eval(logger.info("Loaded config, initializing bot..."))
      bormClient = new BormClient[IO](loggingSttpClient, config.borm)
      datasource <- DataSourceHelpers.createDataSource[IO](config.database)
      flywayLoader     = new FlywayLoader[IO](datasource)
      transactor       = Transactor.fromDataSource[IO](datasource, ec)
      userStateStorage = new UserStateStorage[IO](transactor)
      commandHandler   = new BormCommandHandler[IO](bormClient, config.borm)
      bot = new TelebormBot[IO](
        loggingSttpClient,
        config.telegram.token,
        commandHandler,
        userStateStorage
      )
      botInitializer = new TelebormBotInit[IO](bot, config.telegram)
      _ <- Resource.eval(flywayLoader.load)
      _ <- Resource.eval(botInitializer.setup(args))
    } yield ExitCode.Success

    program.useForever
  }
}
