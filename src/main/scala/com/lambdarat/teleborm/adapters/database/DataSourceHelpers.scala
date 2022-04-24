package com.lambdarat.teleborm.database

import com.lambdarat.teleborm.config.DatabaseMode
import com.lambdarat.teleborm.config.TelebormDatabaseConfig

import javax.sql.DataSource

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolDataSourceFactory
import org.h2.jdbcx.{JdbcConnectionPool => H2JdbcConnectionPool}

object DataSourceHelpers {
  def createDataSource[F[_]: Async](config: TelebormDatabaseConfig): Resource[F, DataSource] =
    config.mode match {
      case DatabaseMode.Memory =>
        val alloc =
          Async[F].delay(H2JdbcConnectionPool.create(config.url, config.user, config.password))
        val free = (ds: H2JdbcConnectionPool) => Async[F].delay(ds.dispose())
        Resource.make(alloc)(free)

      case DatabaseMode.Production | DatabaseMode.Integration =>
        val connectionPoolName = "teleborm"

        val alloc = Async[F].delay {
          val pds = PoolDataSourceFactory.getPoolDataSource
          pds.setConnectionPoolName(connectionPoolName)
          pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource")
          pds.setURL(config.url)
          pds.setUser(config.user)
          pds.setPassword(config.password)
          pds
        }

        val free = (ds: PoolDataSource) =>
          Async[F].delay(
            UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager
              .destroyConnectionPool(ds.getConnectionPoolName)
          )
        Resource.make(alloc)(free)
    }
}
