import cats.data.Ior.Left
import cats.effect.{ConcurrentEffect, ContextShift, Effect, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.effect.concurrent.Ref
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.server.Router
import sttp.tapir.docs.openapi._
import org.http4s.server.middleware.Metrics
import org.http4s.{EntityBody, Http, HttpRoutes, Request, Response, _}
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.{path, _}
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import java.io.StringWriter

import cats.implicits._
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.server.Router
import sttp.tapir.docs.openapi._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir._
import sttp.tapir.server.http4s._
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import org.http4s.server.blaze.BlazeServerBuilder
import sttp.tapir.{endpoint, jsonBody, stringBody}
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats.data.Kleisli
import cats.effect.{ContextShift, Effect, IO, Resource, Sync, Timer}
import cats.implicits._

import org.http4s.implicits._
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.ExecutionContext

package object httpEndpoints {

  implicit val serviceInfoReader: Codec[Note] = deriveCodec
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO]           = IO.contextShift(scala.concurrent.ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ec)
  val notes: Ref[IO, Map[String, Note]]       = Ref.unsafe[IO, Map[String, Note]](Map.empty)

  val createNote: ServerEndpoint[Note, Unit, Unit, Nothing, IO] =
    endpoint.post.in("notes/add").in(jsonBody[Note]).serverLogic(note => addNoteLogic(note))

  private def addNoteLogic(note: Note): IO[Either[Unit, Unit]] = IO {
    if (notes.get.map(_.contains(note.id)).unsafeRunSync())
      Left(notes.set(Map[String, Note](note.id, note)))
    Right(println("error"))
  }

  private val registry = {
    DefaultExports.initialize()
    CollectorRegistry.defaultRegistry
  }

  def httpRoutes(
      endpoints: ServerEndpoint[_, _, _, EntityBody[IO], IO]*
  ): HttpRoutes[IO] = {
    val serviceEndpoints = List(createNote)
    serviceEndpoints.toRoutes
  }

//  def buildService(
//      endpoints: ServerEndpoint[_, _, _, EntityBody[IO], IO]*
//  ): Resource[IO, HttpRoutes[IO]] = {
//    val serviceEndpoints = List(createNote)
//    val allEndpoints     = endpoints.map(_.tag("Api")) ++ serviceEndpoints.map(_.tag("Service"))
//    val docs             = allEndpoints.toOpenAPI("title", "veresion")
//    val docsRoute        = new SwaggerHttp4s(docs.toYaml).routes[IO]
//    val routes           = List(docsRoute, allEndpoints.toList.toRoutes)
//    Prometheus.metricsOps[IO](registry, "server").map(ops => Metrics[IO](ops)(routes.foldLeft(Router[IO]())(_ <+> _)))
//  }

  def noteService: Resource[IO, HttpRoutes[IO]] = {
    val serviceEndpoints = List(createNote)
    val allEndpoints     = serviceEndpoints.map(_.tag("Service"))
    val docs             = allEndpoints.toOpenAPI("title", "veresion")
    val docsRoute        = new SwaggerHttp4s(docs.toYaml).routes[IO]
    val routes           = List(docsRoute, allEndpoints.toRoutes)
    Prometheus.metricsOps[IO](registry, "server").map(ops => Metrics[IO](ops)(routes.foldLeft(Router[IO]())(_ <+> _)))
  }

}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpEndpoints.noteService.orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
