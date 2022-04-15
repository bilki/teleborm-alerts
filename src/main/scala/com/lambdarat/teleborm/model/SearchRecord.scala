package com.lambdarat.teleborm.model

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
    s"https://www.borm.es/\\#/home/anuncio/${SearchRecord.shortPublishedFormat.format(publishedOn)}/$announceId"

  private lazy val bormPdf: String =
    s"https://www.borm.es/services/anuncio/ano/${publishedOn.getYear}/numero/${announceId}/pdf\\?id\\=${pdfId}"

  def pretty: String =
    s"""|${"Publicado".bold}: ${SearchRecord.spanishLongDateFormat.format(publishedOn.toLocalDate)}
        |${"Anuncio".bold}: $bormHtml
        |${"PDF".bold}: $bormPdf
        |${SearchRecord.extendedMarkdownEscape(summary).italic}
        |""".stripMargin

}

object SearchRecord {
  val shortPublishedFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MM-yyyy")

  val spanishLongDateFormat: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern("EEEE',' d 'de' MMMM',' yyyy")
      .localizedBy(Locale.forLanguageTag("es-ES"))

  def extendedMarkdownEscape(text: String): String = text
    .replace("+", "\\+")
    .replace("(", "\\(")
    .replace(")", "\\)")
    .replace("#", "\\#")
}
