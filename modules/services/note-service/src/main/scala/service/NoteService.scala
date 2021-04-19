package service

import cats.data.Kleisli
import models.Note
import repo.{DBNoteRepo, NoteRepo}
import sttp.tapir.Endpoint
import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.syntax.all._
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.server.Router
import org.http4s.server.middleware.Metrics
import sttp.model.StatusCode
import sttp.tapir.docs.openapi._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir.{endpoint, jsonBody, _}
import sttp.model.StatusCode.{Conflict, NotFound}
import io.chrisdavenport.log4cats.Logger
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import utils.postgres.model.DBConfig


class NoteService(
    implicit val cs: ContextShift[IO],
    implicit val timer: Timer[IO],
    implicit val logger: Logger[IO]
) {
  type Err = (String, StatusCode)

  val baseApiEndpoint: Endpoint[Unit, String, Unit, Nothing] =
    endpoint.in("notes").errorOut(jsonBody[String])

  def createNoteEndPoint(repo: NoteRepo): ServerEndpoint[Note, Err, String, Nothing, IO] =
    baseApiEndpoint.post
      .in("create")
      .in(jsonBody[Note])
      .errorOut(statusCode)
      .out(jsonBody[String])
      .serverLogic { note =>
        repo.createNote(note).map(_.left.map(str => str -> Conflict))
      }

  def readNoteEndPoint(repo: NoteRepo): ServerEndpoint[String, Err, Note, Nothing, IO] =
    baseApiEndpoint.get
      .in("read")
      .in(query[String]("id"))
      .errorOut(statusCode)
      .out(jsonBody[Note])
      .serverLogic { noteID =>
        repo.readNote(noteID).map(_.left.map(str => str -> NotFound))
      }

  def updateNoteEndPoint(repo: NoteRepo): ServerEndpoint[Note, Err, String, Nothing, IO] =
    baseApiEndpoint.post
      .in("update")
      .in(jsonBody[Note])
      .errorOut(statusCode)
      .out(jsonBody[String])
      .serverLogic { note =>
        repo.updateNote(note).map(_.left.map(str => str -> NotFound))
      }

  def deleteNoteEndPoint(repo: NoteRepo): ServerEndpoint[String, Err, String, Nothing, IO] =
    baseApiEndpoint.delete
      .in("delete")
      .in(query[String]("id"))
      .errorOut(statusCode)
      .out(jsonBody[String])
      .serverLogic { noteID =>
        repo.deleteNote(noteID).map(_.left.map(str => str -> NotFound))
      }

  def listNoteEndPoint(repo: NoteRepo): ServerEndpoint[Unit, String, List[Note], Nothing, IO] =
    baseApiEndpoint.get
      .in("list")
      .out(jsonBody[List[Note]])
      .serverLogic { _ =>
        repo.listNote()
      }

  private val registry = {
    DefaultExports.initialize()
    CollectorRegistry.defaultRegistry
  }

  def httpService(repo: NoteRepo): Resource[IO, HttpRoutes[IO]] = {
    val serviceEndpoints =
      List(
        createNoteEndPoint(repo),
        readNoteEndPoint(repo),
        updateNoteEndPoint(repo),
        deleteNoteEndPoint(repo),
        listNoteEndPoint(repo)
      ).map(
        _.tag("Service")
      )
    val docs      = serviceEndpoints.toOpenAPI("title", "version")
    val docsRoute = new SwaggerHttp4s(docs.toYaml).routes[IO]
    val routes    = List(docsRoute, serviceEndpoints.toRoutes)
    Prometheus.metricsOps[IO](registry, "server").map(ops => Metrics[IO](ops)(routes.foldLeft(Router[IO]())(_ <+> _)))
  }

  def httpApp(config: DBConfig)(
      implicit cs: ContextShift[IO],
      logger: Logger[IO]
  ): Resource[IO, Kleisli[IO, Request[IO], Response[IO]]] =
    for {
      repo    <- DBNoteRepo(config)
      service <- httpService(repo)
    } yield service.orNotFound
}
