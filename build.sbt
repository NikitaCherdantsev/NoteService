import com.typesafe.sbt.packager.docker.DockerChmodType

scalaVersion := "2.13.1"

lazy val commonSettings = Seq(
  version := "0.0.1-SNAPSHOT",
  autoCompilerPlugins := true,
  addCompilerPlugin(Dependencies.betterMonadicFor)
)

lazy val root = (project in file("."))
  .aggregate(noteService, postgres, logging)
  .settings(
    name := "crude-service",
    skip in publish := true
  )

// Start services

lazy val noteService = project
  .in(file("modules/services/note-service"))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .dependsOn(postgres, logging)
  .settings(commonSettings: _*)
  .settings(
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-Xfatal-warnings",
      "-Ypartial-unification",
      "-feature",
      "-language:higherKinds"
    )
  )
  .settings(name := "note-service",
    libraryDependencies += Dependencies.pureconfig,
    libraryDependencies += Dependencies.pureconfigCatsEffect,
    libraryDependencies += Dependencies.dockerClient,
    libraryDependencies += Dependencies.logbackClassic,
    libraryDependencies += Dependencies.http4sBlazeServer,
    libraryDependencies += Dependencies.http4sCirce,
    libraryDependencies += Dependencies.http4sDsl,
    libraryDependencies += Dependencies.http4sPrometheusMetrics,
    libraryDependencies += Dependencies.tapirCore,
    libraryDependencies += Dependencies.tapirJsonCirce,
    libraryDependencies += Dependencies.tapirSwaggerUiHttp4s,
    libraryDependencies += Dependencies.tapirOpenApiDocs,
    libraryDependencies += Dependencies.tapirOpenapiCirceYaml,
    libraryDependencies += Dependencies.tapirHttp4sServer,
    libraryDependencies += Dependencies.log4catsCore,
    libraryDependencies += Dependencies.log4catsSlf4j,
    libraryDependencies += Dependencies.jawnAst,
    libraryDependencies += Dependencies.scalaLogging,
    libraryDependencies += Dependencies.doobieQuill,
    libraryDependencies += Dependencies.doobieCore
  )
  .settings(
    mappings in Universal += ((resourceDirectory in Compile).value / "application.conf") -> "conf/application.conf",
    mappings in Universal += ((resourceDirectory in Compile).value / "start.sh")         -> "start.sh",
    mappings in Universal += ((resourceDirectory in Compile).value / "stop.sh")          -> "stop.sh",
    scriptClasspath ~= (cp => "../conf" +: cp),
    javaOptions in Universal += "-J-Xmx4G",
    dockerBaseImage := "openjdk:11.0.6-jre-slim",
    dockerExposedPorts := Seq(8080),
    daemonUser in Docker := "naumen",
    dockerChmodType := DockerChmodType.UserGroupWriteExecute,
    dockerExposedVolumes += "/opt/docker/logs",
    dockerUpdateLatest := true,
    skip in publish := true
  )

// End services

// Start utils

lazy val postgres = (project in file("modules/utils/postgres"))
  .settings(commonSettings: _*)
  .settings(
    name := "postgres",
    libraryDependencies += Dependencies.pureconfig,
    libraryDependencies += Dependencies.pureconfigCatsEffect,
    libraryDependencies += Dependencies.circeGeneric,
    libraryDependencies += Dependencies.doobieCore,
    libraryDependencies += Dependencies.doobiePostgres,
    libraryDependencies += Dependencies.doobieSpecs2,
    libraryDependencies += Dependencies.doobieQuill,
    libraryDependencies += Dependencies.doobieHikari,
    libraryDependencies += Dependencies.hikariCP,
    libraryDependencies += Dependencies.tapirJsonCirce,
    libraryDependencies += Dependencies.liqubase
  )

lazy val logging = (project in file("modules/utils/logging"))
  .settings(commonSettings: _*)
  .settings(
    name := "logging",
    libraryDependencies += Dependencies.log4catsCore,
    libraryDependencies += Dependencies.log4catsSlf4j
  )

// End utils
