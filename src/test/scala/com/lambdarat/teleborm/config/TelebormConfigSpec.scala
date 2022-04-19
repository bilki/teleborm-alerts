package com.lambdarat.teleborm.config
import java.util.UUID

import munit.FunSuite
import org.http4s._
import pureconfig.ConfigSource
import sttp.client3.UriContext

class TelebormConfigSpec extends FunSuite {

  test("Config can be read from resource file") {
    val telegramConfig = TelegramConfig(
      Uri.unsafeFromString("example.com"),
      "your_telegram_token"
    )

    val bormConfig = BormConfig(
      uri"https://datosabiertos.regiondemurcia.es/catalogo/api/action/datastore_search",
      resourceId = UUID.fromString("36552a73-2f7a-48a7-9da8-08360c81c29d"),
      limit = 5
    )

    val databaseConfig = TelebormDatabaseConfig(
      url = "jdbc:h2:mem:teleborm;MODE=Oracle",
      "",
      "",
      DatabaseMode.Memory
    )

    val expected = TelebormConfig(
      telegramConfig,
      bormConfig,
      databaseConfig
    )

    val attemptConfigLoad = ConfigSource.default.load[TelebormConfig]

    attemptConfigLoad
      .fold(
        err => fail(err.prettyPrint()),
        config => assertEquals(expected, config)
      )

  }

}
