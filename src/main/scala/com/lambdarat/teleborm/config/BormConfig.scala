package com.lambdarat.teleborm.config

import java.util.UUID

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._
import pureconfig.module.sttp._
import sttp.model.Uri

final case class BormConfig(
    uri: Uri,
    resourceId: UUID
)

object BormConfig {
  implicit val bormConfigReader: ConfigReader[BormConfig] =
    deriveReader[BormConfig]
}
