ThisBuild / scalaVersion := "2.13.8"

lazy val root = project
  .in(file("."))
  .settings(
    name := "teleborm-alert",
    description := "Telegram bot to alert when new releases of the official Region de Murcia government bulletin contains a certain set of words",
    organization := "com.lambdarat",
    version      := "0.1.0",
    scalacOptions ++= Seq("-Wunused", "-deprecation"),
    libraryDependencies ++= Seq(
      "org.typelevel"                 %% "cats-effect"                   % "3.3.11",
      "com.bot4s"                     %% "telegram-core"                 % "5.4.1",
      "com.bot4s"                     %% "telegram-akka"                 % "5.4.1",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2" % "3.5.1",
      "org.http4s"                    %% "http4s-core"                   % "0.23.11",
      "org.http4s"                    %% "http4s-dsl"                    % "0.23.11",
      "org.http4s"                    %% "http4s-circe"                  % "0.23.11",
      "org.http4s"                    %% "http4s-ember-server"           % "0.23.11",
      "org.scalameta"                 %% "munit"                         % "0.7.29" % Test
    )
  )
