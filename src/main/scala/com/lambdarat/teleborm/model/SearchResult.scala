package com.lambdarat.teleborm.model

import com.lambdarat.teleborm.bot.Messages

final case class SearchResult(records: List[SearchRecord], total: Int) {

  def pretty: String =
    s"""
      |${records.map(_.pretty).mkString("\n")}
      |${Messages.foundSearchResults} \\[${total}\\]
      |""".stripMargin

}
