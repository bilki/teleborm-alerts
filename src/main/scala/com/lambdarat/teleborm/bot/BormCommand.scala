package com.lambdarat.teleborm.bot

import com.lambdarat.teleborm.domain.model.BormCommandType

import java.time.LocalDate

import cats.syntax.all._

sealed abstract class BormCommand

object BormCommand {
  case class SaveAlert(userId: Long, words: List[String])                    extends BormCommand
  case class DeleteAlert(userId: Long, alertId: Long)                        extends BormCommand
  case class MyAlerts(userId: Long)                                          extends BormCommand
  case class Search(words: List[String], page: Int, from: Option[LocalDate]) extends BormCommand

  def extractFrom(cbData: Option[String]): Option[BormCommand] = {
    val maybeBormCommand = for {
      rawData <- cbData
      rawParts = rawData.split(":")
      rawCommandType <- rawParts.headOption
      commandType    <- BormCommandType.withNameOption(rawCommandType)
    } yield commandType match {
      case BormCommandType.Search =>
        rawParts.toList.tail match {
          case page :: rawWords :: Nil =>
            Search(rawWords.split(",").toList, page.toIntOption.getOrElse(0), none[LocalDate]).some
          case _ => none[BormCommand]
        }
      case _ => none[BormCommand]
    }

    maybeBormCommand.flatten
  }
}
