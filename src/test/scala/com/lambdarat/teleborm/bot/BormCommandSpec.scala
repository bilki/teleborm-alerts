package com.lambdarat.teleborm

import com.lambdarat.teleborm.bot.BormCommand
import com.lambdarat.teleborm.bot.BormCommandType

import java.time.LocalDate

import cats.syntax.all._
import munit.FunSuite

class BormCommandSpec extends FunSuite {

  test("A callback command is extracted correctly from a callback String") {
    val rawSearchCallbackData = s"${BormCommandType.Search.entryName}:0:agua,vivienda"

    val expected = BormCommand
      .Search(
        words = List("agua", "vivienda"),
        page = 0,
        from = none[LocalDate]
      )
      .some

    val result = BormCommand.extractFrom(rawSearchCallbackData.some)

    assertEquals(result, expected)
  }

  test("A callback command is not extracted from a bogus callback String") {
    val rawSearchCallbackData = "empty"

    val expected = none[BormCommand]

    val result = BormCommand.extractFrom(rawSearchCallbackData.some)

    assertEquals(result, expected)
  }

}
