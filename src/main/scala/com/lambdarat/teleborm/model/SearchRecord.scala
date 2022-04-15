package com.lambdarat.teleborm.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.bot4s.telegram.Implicits._

final case class SearchRecord(summary: String, publishedOn: LocalDateTime, announceId: Long) {

  private lazy val bormHtml: String =
    s"https://www.borm.es/#/home/anuncio/${SearchRecord.shortPublishedFormat.format(publishedOn)}/$announceId"

  def pretty: String =
    s"""|${"Publicado".bold}: ${publishedOn.toLocalDate.toString}
        |${"Enlace".bold}: ${"HTML".altWithUrl(bormHtml)}
        |${summary.italic}
        |""".stripMargin

}

object SearchRecord {
  val shortPublishedFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MM-YYYY")
}
