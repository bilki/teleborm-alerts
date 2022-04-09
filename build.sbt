ThisBuild / scalaVersion := "2.13.8"

lazy val root = project
  .in(file("."))
  .settings(
    name := "teleborm-alert",
    description := "Telegram bot to alert when new releases of the official Region de Murcia government bulletin contains a certain set of words",
    organization := "com.lambdarat",
    version      := "0.1.0",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.3.11",
      "org.scalameta" %% "munit"       % "0.7.29" % Test
    )
  )
