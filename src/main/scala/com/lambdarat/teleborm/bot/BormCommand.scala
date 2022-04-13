package com.lambdarat.teleborm.bot

import java.time.LocalDate

sealed abstract class BormCommand

object BormCommand {
  case class SaveAlert(userId: Long, words: List[String])         extends BormCommand
  case class DeleteAlert(userId: Long, alertId: Long)             extends BormCommand
  case class MyAlerts(userId: Long)                               extends BormCommand
  case class Search(words: List[String], from: Option[LocalDate]) extends BormCommand
}
