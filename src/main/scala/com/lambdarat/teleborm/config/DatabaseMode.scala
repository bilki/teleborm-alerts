package com.lambdarat.teleborm.config

import cats.syntax.all._
import enumeratum.Enum
import enumeratum.EnumEntry
import enumeratum.EnumEntry.Lowercase
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

sealed trait DatabaseMode extends EnumEntry with Lowercase

object DatabaseMode extends Enum[DatabaseMode] {
  val values = findValues

  case object Memory      extends DatabaseMode
  case object Integration extends DatabaseMode
  case object Production  extends DatabaseMode

  implicit val databaseModeConfigReader: ConfigReader[DatabaseMode] =
    ConfigReader.stringConfigReader.emap(rawMode =>
      DatabaseMode
        .withNameEither(rawMode)
        .leftMap(err =>
          CannotConvert(because = err.getMessage, toType = "DatabaseMode", value = rawMode)
        )
    )
}
