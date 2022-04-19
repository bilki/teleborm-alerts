package com.lambdarat.teleborm

import com.lambdarat.teleborm.bot.TelebormBot
import com.lambdarat.teleborm.bot.TelebormBotInit
import com.lambdarat.teleborm.client.BormClient
import com.lambdarat.teleborm.config.DatabaseMode
import com.lambdarat.teleborm.config.TelebormConfig
import com.lambdarat.teleborm.config.TelebormDatabaseConfig
import com.lambdarat.teleborm.database.FlywayLoader
import com.lambdarat.teleborm.handler.BormCommandHandler

import javax.sql.DataSource

import scala.concurrent.ExecutionContext

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolDataSourceFactory
import org.h2.jdbcx.{JdbcConnectionPool => H2JdbcConnectionPool}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig._
import pureconfig.module.catseffect.syntax._
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.logging.slf4j.Slf4jLoggingBackend

object Main extends IOApp {
  implicit val ec = ExecutionContext.global

  private def createDataSource(config: TelebormDatabaseConfig): Resource[IO, DataSource] =
    config.mode match {
      case DatabaseMode.Memory =>
        val alloc =
          IO.delay(H2JdbcConnectionPool.create(config.url, config.user, config.password))
        val free = (ds: H2JdbcConnectionPool) => IO.delay(ds.dispose())
        Resource.make(alloc)(free)

      case DatabaseMode.Production | DatabaseMode.Integration =>
        val connectionPoolName = "teleborm"

        val alloc = IO.delay {
          val pds = PoolDataSourceFactory.getPoolDataSource
          pds.setConnectionPoolName(connectionPoolName)
          pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource")
          pds.setURL(config.url)
          pds.setUser(config.user)
          pds.setPassword(config.password)
          pds
        }

        val free = (ds: PoolDataSource) =>
          IO.delay(
            UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager
              .destroyConnectionPool(ds.getConnectionPoolName)
          )
        Resource.make(alloc)(free)
    }

  def run(args: List[String]): IO[ExitCode] = {
    val program = for {
      implicit0(logger: Logger[IO]) <- Resource.eval(Slf4jLogger.create[IO])
      _      <- Resource.eval(logger.info("Created logger, attempting to create client..."))
      client <- AsyncHttpClientFs2Backend.resource[IO]()
      loggingSttpClient = Slf4jLoggingBackend(client)
      config <- Resource.eval(ConfigSource.default.loadF[IO, TelebormConfig]())
      _      <- Resource.eval(logger.info("Loaded config, initializing bot..."))
      bormClient     = new BormClient[IO](loggingSttpClient, config.borm)
      commandHandler = new BormCommandHandler[IO](bormClient, config.borm)
      datasource <- createDataSource(config.database)
      flywayLoader = new FlywayLoader[IO](datasource)
      bot = new TelebormBot[IO](
        loggingSttpClient,
        config.telegram.token,
        commandHandler
      )
      botInitializer = new TelebormBotInit[IO](bot, config.telegram)
      _ <- Resource.eval(flywayLoader.load)
      _ <- Resource.eval(botInitializer.setup(args))
    } yield ExitCode.Success

    program.useForever
  }
}
