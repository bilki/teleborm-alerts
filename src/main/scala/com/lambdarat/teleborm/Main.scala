package com.lambdarat.teleborm

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import cats.syntax.all._

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO.println("Hello world!").as(ExitCode.Success)
}
