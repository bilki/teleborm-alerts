package com.lambdarat.teleborm

import com.lambdarat.teleborm.bot.TelebormBot
import com.lambdarat.teleborm.bot.TelebormBotInit
import com.lambdarat.teleborm.client.BormClient
import com.lambdarat.teleborm.config.DatabaseMode
import com.lambdarat.teleborm.config.TelebormConfig
import com.lambdarat.teleborm.config.TelebormDatabaseConfig
import com.lambdarat.teleborm.database.FlywayLoader
import com.lambdarat.teleborm.handler.BormCommandHandler

import scala.concurrent.ExecutionContext

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import doobie.h2.H2Transactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig._
import pureconfig.module.catseffect.syntax._
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.logging.slf4j.Slf4jLoggingBackend

object Main extends IOApp {
  implicit val ec = ExecutionContext.global

  private def chooseTransactor(
      config: TelebormDatabaseConfig
  )(implicit ec: ExecutionContext): Resource[IO, Transactor[IO]] = {
    config.mode match {
      case DatabaseMode.Memory | DatabaseMode.Integration =>
        H2Transactor.newH2Transactor[IO](config.url, config.user, config.password, ec)
      case DatabaseMode.Production =>
        val xa = Transactor
          .fromDriverManager[IO](
            "oracle.jdbc.OracleDriver",
            config.url,
            config.user,
            config.password
          )
        Resource.pure(xa)
    }
  }

  def run(args: List[String]): IO[ExitCode] = {
    val program = for {
      implicit0(logger: Logger[IO]) <- Resource.eval(Slf4jLogger.create[IO])
      _      <- Resource.eval(logger.info("Created logger, attempting to create client..."))
      client <- AsyncHttpClientFs2Backend.resource[IO]()
      loggingSttpClient = Slf4jLoggingBackend(client)
      config <- Resource.eval(ConfigSource.default.loadF[IO, TelebormConfig]())
      _      <- Resource.eval(logger.info("Loaded config, initializing bot..."))
      flywayLoader   = new FlywayLoader[IO](config.database)
      bormClient     = new BormClient[IO](loggingSttpClient, config.borm)
      commandHandler = new BormCommandHandler[IO](bormClient, config.borm)
      transactor <- chooseTransactor(config.database)
      yup = sql"select user_id from user_state".query[Int].to[List]
      bot = new TelebormBot[IO](
        loggingSttpClient,
        config.telegram.token,
        commandHandler
      )
      botInitializer = new TelebormBotInit[IO](bot, config.telegram)
      _    <- Resource.eval(flywayLoader.load)
      _    <- Resource.eval(botInitializer.setup(args))
      list <- Resource.eval(yup.transact(transactor))
      _    <- Resource.eval(logger.info(s"Rows: ${list}"))
    } yield ExitCode.Success

    program.useForever
  }
}
