import cats.data.Kleisli
import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpRoutes, Request, Response}

object Main extends IOApp {

  val notes: Ref[IO, Map[String, Note]] = Ref.unsafe(Map.empty)
  val service: Resource[IO, HttpRoutes[IO]] = new HttpEndpoints(notes).noteService

  def run(args: List[String]): IO[ExitCode] = {
    service.map(_.orNotFound).use(application)
  }

  def application(app: Kleisli[IO, Request[IO], Response[IO]]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8083, "localhost")
      .withHttpApp(app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
