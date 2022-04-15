package com.lambdarat.teleborm.model

import com.bot4s.telegram.Implicits._

final case class SearchResult(records: List[SearchRecord], total: Int) {

  def pretty: String =
    s"""${"Resultados encontrados".bold} \\[${total}\\]:
    |
    |${records.map(_.pretty).mkString("\n")}
    |
    |""".stripMargin

}
