package com.lambdarat.teleborm.database

import javax.sql.DataSource

import cats.effect.kernel.Async
import cats.syntax.all._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.output.MigrateResult

class FlywayLoader[F[_]: Async](dataSource: DataSource) {
  def load: F[MigrateResult] =
    for {
      flyway <- Async[F].delay(
        Flyway.configure
          .dataSource(dataSource)
          .target(MigrationVersion.LATEST)
          .load
      )
      result <- Async[F].delay(flyway.migrate)
    } yield result
}
