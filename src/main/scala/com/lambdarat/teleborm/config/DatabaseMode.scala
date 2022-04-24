package com.lambdarat.teleborm.config

import enumeratum.Enum
import enumeratum.EnumEntry
import enumeratum.EnumEntry.Lowercase
import pureconfig._
import pureconfig.module.enumeratum.enumeratumConfigConvert

sealed trait DatabaseMode extends EnumEntry with Lowercase

object DatabaseMode extends Enum[DatabaseMode] {
  val values = findValues

  case object Memory      extends DatabaseMode
  case object Integration extends DatabaseMode
  case object Production  extends DatabaseMode

  implicit val databaseModeConfigReader: ConfigReader[DatabaseMode] =
    enumeratumConfigConvert[DatabaseMode]
}
