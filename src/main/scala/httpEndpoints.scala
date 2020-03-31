import cats.data.Ior.Left
import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits._
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax._
import io.circe.{Codec, Json}
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import org.http4s.HttpRoutes
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.server.Router
import org.http4s.server.middleware.Metrics
import sttp.tapir.docs.openapi._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir.{endpoint, jsonBody, _}

import scala.concurrent.ExecutionContext

package object httpEndpoints {

  implicit val serviceInfoReader: Codec[Note] = deriveCodec
  implicit val ec: ExecutionContext           = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO]           = IO.contextShift(scala.concurrent.ExecutionContext.global)
  implicit val timer: Timer[IO]               = IO.timer(ec)
  val notes: Ref[IO, Map[String, Note]]       = Ref.unsafe[IO, Map[String, Note]](Map.empty)

  val createNote: ServerEndpoint[Note, String, String, Nothing, IO] =
    endpoint.post
      .in("notes/add")
      .in(jsonBody[Note])
      .errorOut(jsonBody[String])
      .out(jsonBody[String])
      .serverLogic(note => createNoteLogic(note))

  val readNote: ServerEndpoint[String, String, Json, Nothing, IO] =
    endpoint.get
      .in("notes/read")
      .in(jsonBody[String])
      .errorOut(jsonBody[String])
      .out(jsonBody[Json])
      .serverLogic(id => readNoteLogic(id))

  val updateNote: ServerEndpoint[Note, String, String, Nothing, IO] =
    endpoint.post
      .in("notes/update")
      .in(jsonBody[Note])
      .errorOut(jsonBody[String])
      .out(jsonBody[String])
      .serverLogic(note => updateNoteLogic(note))

  val deleteNote: ServerEndpoint[String, String, String, Nothing, IO] =
    endpoint.delete
      .in("notes/delete")
      .in(jsonBody[String])
      .errorOut(jsonBody[String])
      .out(jsonBody[String])
      .serverLogic(id => deleteNoteLogic(id))

  val listNote: ServerEndpoint[Unit, String, Json, Nothing, IO] =
    endpoint.get
      .in("notes/list")
      .errorOut(jsonBody[String])
      .out(jsonBody[Json])
      .serverLogic(unit => listNoteLogic)

  private def createNoteLogic(note: Note): IO[Either[String, String]] = IO {
    if (!notes.get.map(_.isDefinedAt(note.id)).unsafeRunSync())
      Left("This Note is already exists!")
    notes.set(Map[String, Note](note.id -> note))
    Right("Aded!")
  }

  def readNoteLogic(id: String): IO[Either[String, Json]] = IO {
    if (!notes.get.map(_.isDefinedAt(id)).unsafeRunSync())
      Left("This ID is doesn't exists!")
    Right(notes.get.unsafeRunSync()(id).asJson)
  }

  def updateNoteLogic(note: Note): IO[Either[String, String]] = IO {
    if (!notes.get.map(_.isDefinedAt(note.id)).unsafeRunSync())
      Left("This ID is doesn't exists!")
    notes.update(map => map.updated(note.id, note))
    Right("Note was updated!")
  }

  def deleteNoteLogic(id: String): IO[Either[String, String]] = IO {
    if (!notes.get.map(_.isDefinedAt(id)).unsafeRunSync())
      Left("This ID is doesn't exists!")
    notes.update(map => map.removed(id))
    Right("Removed!")
  }

  def listNoteLogic: IO[Either[String, Json]] = IO {
    if (notes.get.map(_.isEmpty).unsafeRunSync())
      Left("Empty!")
    Right(notes.get.unsafeRunSync().toList.asJson)
  }

  private val registry = {
    DefaultExports.initialize()
    CollectorRegistry.defaultRegistry
  }

  def noteService: Resource[IO, HttpRoutes[IO]] = {
    val serviceEndpoints = List(createNote, readNote, updateNote, deleteNote, listNote).map(_.tag("Service"))
    val docs             = serviceEndpoints.toOpenAPI("title", "veresion")
    val docsRoute        = new SwaggerHttp4s(docs.toYaml).routes[IO]
    val routes           = List(docsRoute, serviceEndpoints.toRoutes)
    Prometheus.metricsOps[IO](registry, "server").map(ops => Metrics[IO](ops)(routes.foldLeft(Router[IO]())(_ <+> _)))
  }
}
