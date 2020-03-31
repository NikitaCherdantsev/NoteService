import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{Request, Response}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val resource = for {
      routes <- httpEndpoints.noteService
    } yield routes.orNotFound

    resource.use { app =>
      application(app)
    }
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
