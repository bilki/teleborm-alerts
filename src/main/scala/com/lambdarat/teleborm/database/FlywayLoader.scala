package com.lambdarat.teleborm.database

import com.lambdarat.teleborm.config.TelebormDatabaseConfig

import cats.effect.kernel.Async
import cats.syntax.all._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion

class FlywayLoader[F[_]: Async](config: TelebormDatabaseConfig) {
  def load: F[Unit] =
    for {
      flyway <- Async[F].delay(
        Flyway.configure
          .dataSource(config.url, config.user, config.password)
          .target(MigrationVersion.LATEST)
          .load
      )
      _ <- Async[F].delay(flyway.migrate)
    } yield ()
}
