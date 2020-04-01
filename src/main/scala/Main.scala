import cats.data.Kleisli
import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{Request, Response}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      ref     <- Ref.of[IO, Map[String, Note]](Map.empty)
      service <- new HttpEndpoints(ref).noteService.map(_.orNotFound).use(application)
    } yield service

  def application(app: Kleisli[IO, Request[IO], Response[IO]]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8083, "localhost")
      .withHttpApp(app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
