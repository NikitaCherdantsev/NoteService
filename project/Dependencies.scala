import sbt._

object Dependencies {

  object Versions {

    lazy val http4sVersion = "0.21.1"

    lazy val circeVersion = "0.13.0"

    lazy val logbackVersion = "1.2.3"

    lazy val typelevelVersion = "1.0.0"

    lazy val tapirVersion = "0.12.25"

    lazy val scalaloggingVersion = "3.9.2"

    lazy val doobieVersion = "0.8.8"

    lazy val log4catsVersion = "1.0.1"

    lazy val pureconfigVersion = "0.12.3"

    lazy val hikariCPVersion = "3.4.5"

    lazy val doobieHikariVersion = "0.9.0"

    lazy val liquibaseVersion = "4.0.0"

    lazy val dockerClientVersion = "8.9.0"

    lazy val betterMonadicForVersion = "0.3.1"

  }

  import Versions._

  lazy val http4sBlazeServer       = "org.http4s" %% "http4s-blaze-server"       % http4sVersion
  lazy val http4sCirce             = "org.http4s" %% "http4s-circe"              % http4sVersion
  lazy val http4sDsl               = "org.http4s" %% "http4s-dsl"                % http4sVersion
  lazy val http4sPrometheusMetrics = "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion

  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion

  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion

  lazy val jawnAst = "org.typelevel" %% "jawn-ast" % typelevelVersion

  lazy val tapirCore             = "com.softwaremill.sttp.tapir" %% "tapir-core"               % tapirVersion
  lazy val tapirJsonCirce        = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion
  lazy val tapirSwaggerUiHttp4s  = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % tapirVersion
  lazy val tapirOpenApiDocs      = "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % tapirVersion
  lazy val tapirOpenapiCirceYaml = "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion
  lazy val tapirHttp4sServer     = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % tapirVersion

  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaloggingVersion

  lazy val doobieCore     = "org.tpolecat" %% "doobie-core"     % doobieVersion
  lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
  lazy val doobieSpecs2   = "org.tpolecat" %% "doobie-specs2"   % doobieVersion
  lazy val doobieQuill    = "org.tpolecat" %% "doobie-quill"    % doobieVersion

  lazy val log4catsCore  = "io.chrisdavenport" %% "log4cats-core"  % log4catsVersion
  lazy val log4catsSlf4j = "io.chrisdavenport" %% "log4cats-slf4j" % log4catsVersion

  lazy val pureconfig           = "com.github.pureconfig" %% "pureconfig"             % pureconfigVersion
  lazy val pureconfigCatsEffect = "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfigVersion

  lazy val hikariCP = "com.zaxxer" % "HikariCP" % hikariCPVersion

  lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % doobieHikariVersion

  lazy val liqubase = "org.liquibase" % "liquibase-core" % liquibaseVersion

  lazy val dockerClient = "com.spotify" % "docker-client" % dockerClientVersion

  lazy val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % betterMonadicForVersion

}
