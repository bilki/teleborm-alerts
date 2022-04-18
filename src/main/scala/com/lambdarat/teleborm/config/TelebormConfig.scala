package com.lambdarat.teleborm.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

final case class TelebormConfig(
    telegram: TelegramConfig,
    borm: BormConfig,
    database: TelebormDatabaseConfig
)

object TelebormConfig {
  implicit val telebormConfigReader: ConfigReader[TelebormConfig] =
    deriveReader[TelebormConfig]
}
