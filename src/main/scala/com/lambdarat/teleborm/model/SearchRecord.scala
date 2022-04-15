package com.lambdarat.teleborm.model

import com.lambdarat.teleborm.bot.Messages
import com.lambdarat.teleborm.bot.Messages._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.bot4s.telegram.Implicits._

final case class SearchRecord(
    summary: String,
    publishedOn: LocalDateTime,
    announceId: Long,
    pdfId: Long
) {

  private lazy val bormHtml: String =
    s"https://www.borm.es/\\#/home/anuncio/${SearchRecord.shortPublishedDateFormat.format(publishedOn)}/$announceId"

  private lazy val bormPdf: String =
    s"https://www.borm.es/services/anuncio/ano/${publishedOn.getYear}/numero/${announceId}/pdf\\?id\\=${pdfId}"

  private lazy val publishedLongDate =
    SearchRecord.spanishLongDateFormat.format(publishedOn.toLocalDate)

  def pretty: String =
    s"""|${Messages.published}: ${publishedLongDate}
        |${Messages.announce}: $bormHtml
        |${"PDF".bold}: $bormPdf
        |${summary.extendedEscapeMd.italic}
        |""".stripMargin

}

object SearchRecord {
  val shortPublishedDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MM-yyyy")

  val spanishLongDateFormat: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern("EEEE',' d 'de' MMMM',' yyyy")
      .localizedBy(Locale.forLanguageTag("es-ES"))
}
