package com.lambdarat.teleborm.domain.model

import doobie.util.meta.Meta
import enumeratum.Enum
import enumeratum.EnumEntry
import enumeratum.EnumEntry.Snakecase

sealed trait ConversationState extends EnumEntry with Snakecase

object ConversationState extends Enum[ConversationState] {
  def values = findValues

  case object Init              extends ConversationState
  case object AskingSearchWords extends ConversationState
  case object AskingSearchDate  extends ConversationState
  case object AskingAlertWords  extends ConversationState

  implicit val convStateMeta: Meta[ConversationState] =
    Meta[String].timap[ConversationState](ConversationState.withName)(_.entryName)
}
