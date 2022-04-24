package com.lambdarat.teleborm.domain.model

import com.bot4s.telegram.Implicits._

object Messages {
  implicit class EscapeMarkdown(text: String) {
    def escapeMd: String = text
      .replace(".", "\\.")
      .replace("|", "\\|")
      .replace("-", "\\-")

    def extendedEscapeMd: String = text
      .replace("+", "\\+")
      .replace("(", "\\(")
      .replace(")", "\\)")
      .replace("#", "\\#")
  }

  val helpCommandDescription = "Recibe de nuevo el mensaje inicial de ayuda"
  val searchCommandDescription =
    "palabra1 palabra2 palabraN - Busca publicaciones que contengan todas las palabras"
  val searchFromCommandDescription =
    "2022-01-01 palabra1 palabra2 palabraN - Busca publicaciones que contengan todas las palabras desde la fecha indicada"

  val greeting =
    s"""|Bienvenido al buscador y notificador de publicaciones del ${"BORM".bold}.
        |
        |Este pequeño bot permite buscar por palabras clave, así como establecer
        |alertas para recibir mensajes con las nuevas publicaciones diarias.
        |
        |Puedes utilizar los siguientes comandos:
        |
        |/start o /ayuda - Recibe de nuevo este mensaje de ayuda
        |
        |/buscar palabra1 palabra2 palabraN - Busca publicaciones que contengan ${"todas".bold} estas palabras
        |
        |/buscar\\_desde 2022-01-01 palabra1 palabra2 palabraN - Busca publicaciones que contengan ${"todas".bold} estas palabras desde la fecha indicada
        """.stripMargin

  val contact =
    s"No se pudo completar la búsqueda, contacta con ${"\\@bilki".altWithUrl("https://twitter.com/bilki")} en Twitter para más información"

  val missingArgsForSearch = "La búsqueda no funcionará si no se introduce al menos una palabra"

  def invalidDateForSearch(rawDate: String): String =
    s"La fecha proporcionada ${rawDate} no es válida"

  val askForWordsSearch: String =
    s"De acuerdo, introduce varias palabras ${"separadas por espacios".bold} para buscar anuncios que las contengan todas"

  val missingArgsForSearchWithDate =
    "La búsqueda no funcionará si no se introduce al menos la fecha y una palabra"

  val foundSearchResults: String = "Resultados encontrados".bold

  // Search record messages
  val published = "Publicado".bold
  val announce  = "Anuncio".bold

}
