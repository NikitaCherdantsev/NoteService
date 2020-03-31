version := "0.0.1-SNAPSHOT"
scalaVersion := "2.13.1"
libraryDependencies ++= Seq(
  "org.http4s"                  %% "http4s-blaze-server"       % "0.21.1",
  "org.http4s"                  %% "http4s-circe"              % "0.21.1",
  "org.http4s"                  %% "http4s-dsl"                % "0.21.1",
  "io.circe"                    %% "circe-generic"             % "0.13.0",
  "ch.qos.logback"              % "logback-classic"            % "1.2.3",
  "org.typelevel"               %% "jawn-ast"                  % "1.0.0",
  "com.softwaremill.sttp.tapir" %% "tapir-core"                % "0.12.25",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe"          % "0.12.25",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"   % "0.12.25",
  "org.http4s"                  %% "http4s-prometheus-metrics" % "0.21.1",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"        % "0.12.25",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml"  % "0.12.25",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "0.12.25"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-Xfatal-warnings"
)
