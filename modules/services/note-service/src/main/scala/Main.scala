import cats.data.Kleisli
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import models.ServiceConfig
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{Request, Response}
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import service.NoteService
import utils.postgres.model.DBConfig

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
      //      ref <- Ref.of[IO, Map[String, Note]](Map.empty)
      //      service <- new NoteService(new MemoryNoteRepo(ref)).service
      //                  .map(_.orNotFound)
      //                  .use(application) // for memoryNote Service
      config <- Blocker[IO].use(blocker => ConfigSource.default.at("service-config").loadF[IO, ServiceConfig](blocker))
      service <- new NoteService()
                  .httpApp(config.database)
                  .use(app => application(app, config)) // for DBNoteService
    } yield service

  def application(app: Kleisli[IO, Request[IO], Response[IO]], serviceConfig: ServiceConfig): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(serviceConfig.port, serviceConfig.host)
      .withHttpApp(app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
