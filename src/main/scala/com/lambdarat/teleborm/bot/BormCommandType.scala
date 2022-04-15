package com.lambdarat.teleborm.bot

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

sealed trait BormCommandType extends EnumEntry with Lowercase

object BormCommandType extends Enum[BormCommandType] {
  val values = findValues

  case object Help           extends BormCommandType
  case object Search         extends BormCommandType
  case object SearchWithDate extends BormCommandType
  case object SaveAlert      extends BormCommandType
}
