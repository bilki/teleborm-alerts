ThisBuild / scalaVersion                              := "2.13.8"
ThisBuild / semanticdbEnabled                         := true
ThisBuild / semanticdbVersion                         := scalafixSemanticdb.revision
ThisBuild / scalafixDependencies += "com.nequissimus" %% "sort-imports" % "0.6.1"
ThisBuild / assemblyMergeStrategy := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

resolvers += Resolver.sonatypeRepo("snapshots")

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

lazy val root = project
  .in(file("."))
  .settings(
    name := "teleborm-alert",
    description := "Telegram bot to alert when new releases of the official Region de Murcia government bulletin contains a certain set of words",
    organization := "com.lambdarat",
    version      := "0.1.0",
    scalacOptions ++= Seq("-Wunused", "-deprecation"),
    assembly / assemblyJarName := "teleborm-alerts.jar",
    libraryDependencies ++= Seq(
      "ch.qos.logback"                 % "logback-core"                  % "1.2.10",
      "ch.qos.logback"                 % "logback-classic"               % "1.2.10",
      "com.bot4s"                     %% "telegram-core"                 % "5.4.1",
      "com.bot4s"                     %% "telegram-akka"                 % "5.4.1",
      "com.oracle.database.jdbc"       % "ojdbc11"                       % "21.5.0.0",
      "com.oracle.database.jdbc"       % "ucp"                           % "21.5.0.0",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2" % "3.5.2",
      "com.softwaremill.sttp.client3" %% "slf4j-backend"                 % "3.5.2",
      "com.softwaremill.sttp.client3" %% "circe"                         % "3.5.2",
      "com.github.pureconfig"         %% "pureconfig"                    % "0.17.1",
      "com.github.pureconfig"         %% "pureconfig-cats-effect"        % "0.17.1",
      "com.github.pureconfig"         %% "pureconfig-http4s"             % "0.17.1",
      "com.github.pureconfig"         %% "pureconfig-sttp"               % "0.17.1",
      "org.flywaydb"                   % "flyway-core"                   % "8.5.8",
      "org.http4s"                    %% "http4s-core"                   % "0.23.11",
      "org.http4s"                    %% "http4s-dsl"                    % "0.23.11",
      "org.http4s"                    %% "http4s-circe"                  % "0.23.11",
      "org.http4s"                    %% "http4s-ember-server"           % "0.23.11",
      "org.tpolecat"                  %% "doobie-core"                   % "1.0.0-RC2",
      "org.tpolecat"                  %% "doobie-h2"                     % "1.0.0-RC2",
      "org.typelevel"                 %% "cats-effect"                   % "3.3.11",
      "com.beachape"                  %% "enumeratum"                    % "1.7.0",
      "org.typelevel"                 %% "log4cats-slf4j"                % "2.2.0",
      "com.scalawilliam"              %% "letsencrypt-scala"             % "0.0.6-SNAPSHOT",
      "org.scalameta"                 %% "munit"                         % "0.7.29" % Test
    )
  )
