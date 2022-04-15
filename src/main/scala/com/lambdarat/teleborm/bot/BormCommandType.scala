package com.lambdarat.teleborm.bot

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

sealed abstract class BormCommandType(val translation: String) extends EnumEntry with Lowercase

object BormCommandType extends Enum[BormCommandType] {
  val values = findValues

  case object Help           extends BormCommandType("ayuda")
  case object Search         extends BormCommandType("buscar")
  case object SearchWithDate extends BormCommandType("buscar_desde")
  case object SaveAlert      extends BormCommandType("nueva_alerta")
  case object ListAlerts     extends BormCommandType("ver_alertas")
  case object DeleteAlert    extends BormCommandType("borrar_alerta")
}
