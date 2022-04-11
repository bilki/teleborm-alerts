package com.lambdarat.teleborm.config

import org.http4s.Uri
import pureconfig.generic.semiauto._
import pureconfig.module.http4s._
import pureconfig.ConfigReader

case class TelegramConfig(
    webhook: Uri,
    token: String
)

object TelegramConfig {
  implicit val telegramConfigReader: ConfigReader[TelegramConfig] =
    deriveReader[TelegramConfig]
}
