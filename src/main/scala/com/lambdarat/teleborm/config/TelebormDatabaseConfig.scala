package com.lambdarat.teleborm.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

final case class TelebormDatabaseConfig(
    url: String,
    user: String,
    password: String,
    mode: DatabaseMode
)

object TelebormDatabaseConfig {
  implicit val telebormDatabaseConfigReader: ConfigReader[TelebormDatabaseConfig] =
    deriveReader[TelebormDatabaseConfig]
}
