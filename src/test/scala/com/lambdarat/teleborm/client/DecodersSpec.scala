package com.lambdarat.teleborm.client

import com.lambdarat.teleborm.model.SearchRecord
import com.lambdarat.teleborm.model.SearchResult

import java.time.LocalDateTime

import cats.syntax.all._
import decoders._
import io.circe.DecodingFailure
import io.circe.parser._
import munit.FunSuite

class DecodersSpec extends FunSuite {

  private val sampleResponse: String =
    s"""|{
        |    "help": "https://datosabiertos.regiondemurcia.es/catalogo/api/3/action/help_show?name=datastore_search",
        |    "success": true,
        |    "result": {
        |        "sort": "Fec_Publicacion",
        |        "resource_id": "36552a73-2f7a-48a7-9da8-08360c81c29d",
        |        "fields": [
        |            {
        |                "type": "text",
        |                "id": "Sumario"
        |            },
        |            {
        |                "type": "timestamp",
        |                "id": "Fec_Publicacion"
        |            },
        |            {
        |                "type": "numeric",
        |                "id": "ID_Anuncio"
        |            },
        |            {
        |                "type": "numeric",
        |                "id": "ID_Objeto_Digital_Anuncio"
        |            },
        |            {
        |                "type": "int8",
        |                "id": "_full_count"
        |            },
        |            {
        |                "type": "float4",
        |                "id": "rank Sumario"
        |            }
        |        ],
        |        "q": {
        |            "Sumario": "beca"
        |        },
        |        "records": [
        |            {
        |                "_full_count": "9",
        |                "ID_Objeto_Digital_Anuncio": "799553",
        |                "rank Sumario": 0.0573088,
        |                "ID_Anuncio": "535",
        |                "Sumario": "Propuesta de Resolución provisional de concesión de una beca de postgrado en Estadística, a desarrollar en el Centro Regional de Estadística de Murcia, convocada mediante Orden de 7 de julio de 2020, del Consejero de Presidencia y Hacienda.",
        |                "Fec_Publicacion": "2022-02-09T00:00:00"
        |            }
        |        ],
        |        "limit": 1,
        |        "_links": {
        |            "start": "/catalogo/api/action/datastore_search?q=%7B+%22Sumario%22%3A+%22beca%22+%7D&fields=Sumario%2CFec_Publicacion%2CID_Anuncio%2CID_Objeto_Digital_Anuncio&resource_id=36552a73-2f7a-48a7-9da8-08360c81c29d&limit=1&sort=Fec_Publicacion",
        |            "next": "/catalogo/api/action/datastore_search?sort=Fec_Publicacion&resource_id=36552a73-2f7a-48a7-9da8-08360c81c29d&fields=Sumario%2CFec_Publicacion%2CID_Anuncio%2CID_Objeto_Digital_Anuncio&q=%7B+%22Sumario%22%3A+%22beca%22+%7D&limit=1&offset=1"
        |        },
        |        "total": 9
        |    }
        |}
        |""".stripMargin

  test("Decoders parse and decode correctly search results") {
    val expected = SearchResult(
      records = List(
        SearchRecord(
          summary =
            "Propuesta de Resolución provisional de concesión de una beca de postgrado en Estadística, a desarrollar en el Centro Regional de Estadística de Murcia, convocada mediante Orden de 7 de julio de 2020, del Consejero de Presidencia y Hacienda.",
          publishedOn = LocalDateTime.parse("2022-02-09T00:00:00"),
          announceId = 535,
          pdfId = 799553
        )
      ),
      total = 9
    )

    val decoderResult = parse(sampleResponse).flatMap(_.as[SearchResult])

    assertEquals(decoderResult, expected.asRight[DecodingFailure])
  }

  private val noRecordsResponse: String =
    s"""|{
        |    "help": "https://datosabiertos.regiondemurcia.es/catalogo/api/3/action/help_show?name=datastore_search",
        |    "success": true,
        |    "result": {
        |        "sort": "Fec_Publicacion",
        |        "resource_id": "36552a73-2f7a-48a7-9da8-08360c81c29d",
        |        "fields": [
        |            {
        |                "type": "text",
        |                "id": "Sumario"
        |            },
        |            {
        |                "type": "timestamp",
        |                "id": "Fec_Publicacion"
        |            },
        |            {
        |                "type": "numeric",
        |                "id": "ID_Anuncio"
        |            },
        |            {
        |                "type": "numeric",
        |                "id": "ID_Objeto_Digital_Anuncio"
        |            },
        |            {
        |                "type": "int8",
        |                "id": "_full_count"
        |            },
        |            {
        |                "type": "float4",
        |                "id": "rank Sumario"
        |            }
        |        ],
        |        "q": {
        |            "Sumario": "beca"
        |        },
        |        "records": [],
        |        "limit": 1,
        |        "_links": {
        |            "start": "/catalogo/api/action/datastore_search?q=%7B+%22Sumario%22%3A+%22beca%22+%7D&fields=Sumario%2CFec_Publicacion%2CID_Anuncio%2CID_Objeto_Digital_Anuncio&resource_id=36552a73-2f7a-48a7-9da8-08360c81c29d&limit=1&sort=Fec_Publicacion",
        |            "next": "/catalogo/api/action/datastore_search?sort=Fec_Publicacion&resource_id=36552a73-2f7a-48a7-9da8-08360c81c29d&fields=Sumario%2CFec_Publicacion%2CID_Anuncio%2CID_Objeto_Digital_Anuncio&q=%7B+%22Sumario%22%3A+%22beca%22+%7D&limit=1&offset=1"
        |        }
        |    }
        |}
        |""".stripMargin

  test("Decoders should work with no records and return a default total of 0") {
    val expected = SearchResult(
      records = List.empty,
      total = 0
    )

    val decoderResult = parse(noRecordsResponse).flatMap(_.as[SearchResult])

    assertEquals(decoderResult, expected.asRight[DecodingFailure])
  }
}
