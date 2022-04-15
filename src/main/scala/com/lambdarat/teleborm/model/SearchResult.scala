package com.lambdarat.teleborm.model

final case class SearchResult(records: List[SearchRecord]) {

  def pretty: String =
    s"""Resultados \\[${records.size}\\]:
    |
    |${records.map(_.pretty).mkString("\n")}
    |
    |""".stripMargin

}
