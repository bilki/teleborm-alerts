package com.lambdarat.teleborm.bot

import com.bot4s.telegram.models.InlineKeyboardButton
import com.bot4s.telegram.models.InlineKeyboardMarkup

object Pagination {

  def prepareSearchButtons(
      words: List[String],
      currentPage: Int,
      total: Int,
      limit: Int = 5
  ): InlineKeyboardMarkup = {
    val numPages  = Math.ceil(total.toDouble / limit.toDouble)
    val lastPage  = numPages.toInt - 1
    val humanPage = currentPage + 1

    val firstButton =
      Option.when(currentPage > 1)(
        InlineKeyboardButton.callbackData(
          "<< 1",
          s"${BormCommandType.Search.entryName}:0:${words.mkString(",")}"
        )
      )

    val previousButton =
      Option.when(currentPage > 0)(
        InlineKeyboardButton.callbackData(
          s"< ${humanPage - 1}",
          s"${BormCommandType.Search.entryName}:${currentPage - 1}:${words.mkString(",")}"
        )
      )

    val nextButton =
      Option.when(currentPage < lastPage)(
        InlineKeyboardButton.callbackData(
          s"${humanPage + 1} >",
          s"${BormCommandType.Search.entryName}:${currentPage + 1}:${words.mkString(",")}"
        )
      )

    val lastButton =
      Option.when(currentPage < lastPage - 1)(
        InlineKeyboardButton.callbackData(
          s"${lastPage + 1} >>",
          s"${BormCommandType.Search.entryName}:${lastPage}:${words.mkString(",")}"
        )
      )

    val buttons = List(
      firstButton,
      previousButton,
      nextButton,
      lastButton
    ).flatten

    InlineKeyboardMarkup.singleRow(buttons)
  }

}
