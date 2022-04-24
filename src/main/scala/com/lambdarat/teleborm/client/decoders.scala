package com.lambdarat.teleborm.client

import com.lambdarat.teleborm.domain.model.{SearchRecord, SearchResult}

import java.time.LocalDateTime

import cats.syntax.all._
import io.circe.CursorOp
import io.circe.Decoder
import io.circe.DecodingFailure

object decoders {

  implicit val searchRecordDecoder: Decoder[SearchRecord] = Decoder { json =>
    for {
      summary        <- json.downField("Sumario").as[String]
      publishedOnRaw <- json.downField("Fec_Publicacion").as[String]
      publishedOn <- Either
        .catchNonFatal(LocalDateTime.parse(publishedOnRaw))
        .leftMap(err =>
          DecodingFailure.fromThrowable(err, List(CursorOp.DownField("Fec_Publicacion")))
        )
      announceId <- json.downField("ID_Anuncio").as[String].map(_.toLong)
      pdfId      <- json.downField("ID_Objeto_Digital_Anuncio").as[String].map(_.toLong)
    } yield SearchRecord(summary, publishedOn, announceId, pdfId)
  }

  implicit val searchResultDecoder: Decoder[SearchResult] = Decoder { json =>
    for {
      records <- json.downField("result").downField("records").as[List[SearchRecord]]
      total   <- json.downField("result").get[Option[Int]]("total")
    } yield SearchResult(records, total.getOrElse(0))
  }

}
