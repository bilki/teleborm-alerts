package com.lambdarat.teleborm.calendar

import enumeratum.Enum
import enumeratum.EnumEntry
import enumeratum.EnumEntry.Snakecase

sealed trait CalendarAction extends EnumEntry with Snakecase

object CalendarAction extends Enum[CalendarAction] {
  def values = findValues

  case object Ignore    extends CalendarAction
  case object SetYear   extends CalendarAction
  case object PrevYears extends CalendarAction
  case object NextYears extends CalendarAction
  case object Start     extends CalendarAction
  case object SetMonth  extends CalendarAction
  case object SetDay    extends CalendarAction
}
