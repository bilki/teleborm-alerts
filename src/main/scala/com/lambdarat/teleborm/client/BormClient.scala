package com.lambdarat.teleborm.client

import com.lambdarat.teleborm.config.BormConfig
import com.lambdarat.teleborm.model.SearchResult

import cats.effect._
import cats.syntax.all._
import decoders._
import io.circe.Json
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Uri

class BormClient[F[_]: Async](sttp: SttpBackend[F, _], config: BormConfig) {

  def search(words: List[String], page: Int): F[SearchResult] = {
    val offset = page * config.limit

    val uri = config.uri
      .withParams(
        "resource_id" -> config.resourceId.toString,
        "fields"      -> "Sumario,Fec_Publicacion,ID_Anuncio,ID_Objeto_Digital_Anuncio",
        "sort"        -> "Fec_Publicacion desc",
        "q" -> Json
          .obj(
            "Sumario" -> Json.fromString(words.mkString(","))
          )
          .noSpaces,
        "limit"  -> config.limit.toString,
        "offset" -> offset.toString
      )
      .queryValueSegmentsEncoding(Uri.QuerySegmentEncoding.Standard)

    val request = basicRequest.get(uri).response(asJson[SearchResult]).responseGetRight

    sttp.send(request).map(_.body)
  }

}
