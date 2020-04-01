import cats.data.Ior.Left
import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.syntax.all._
import io.circe.Json
import io.circe.syntax._
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import org.http4s.HttpRoutes
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


class httpEndpoints(notes: Ref[IO, Map[String, Note]])(implicit val cs: ContextShift[IO], implicit val timer: Timer[IO]) {
//
//  implicit val ec: ExecutionContext           = scala.concurrent.ExecutionContext.Implicits.global
//  implicit val cs: ContextShift[IO]           = IO.contextShift(scala.concurrent.ExecutionContext.global)
//  implicit val timer: Timer[IO]               = IO.timer(ec)


  def baseApiEndpoint: Endpoint[Unit, Unit, Unit, Nothing] = endpoint.in("notes")

  val createNote: ServerEndpoint[Note, (StatusCode, String), String, Nothing, IO] =
    baseApiEndpoint.post
      .in("add")
      .in(jsonBody[Note])
      .errorOut(statusCode)
      .errorOut(jsonBody[String])
      .out(jsonBody[String])
      .serverLogic(note => createNoteLogic(note))

  val readNote: ServerEndpoint[String, (StatusCode, String), Json, Nothing, IO] =
    baseApiEndpoint.get
      .in("read")
      .in(jsonBody[String])
      .errorOut(statusCode)
      .errorOut(jsonBody[String])
      .out(jsonBody[Json])
      .serverLogic(id => readNoteLogic(id))

  val updateNote: ServerEndpoint[Note, (StatusCode, String), String, Nothing, IO] =
    baseApiEndpoint.post
      .in("update")
      .in(jsonBody[Note])
      .errorOut(statusCode)
      .errorOut(jsonBody[String])
      .out(jsonBody[String])
      .serverLogic(note => updateNoteLogic(note))

  val deleteNote: ServerEndpoint[String, (StatusCode, String), String, Nothing, IO] =
    baseApiEndpoint.delete
      .in("delete")
      .in(jsonBody[String])
      .errorOut(statusCode)
      .errorOut(jsonBody[String])
      .out(jsonBody[String])
      .serverLogic(id => deleteNoteLogic(id))

  val listNote: ServerEndpoint[Unit, (StatusCode, String), Json, Nothing, IO] =
    baseApiEndpoint.get
      .in("list")
      .errorOut(statusCode)
      .errorOut(jsonBody[String])
      .out(jsonBody[Json])
      .serverLogic(unit => listNoteLogic)

  private def createNoteLogic(note: Note): IO[Either[(StatusCode, String), String]] = {
    def logic(option: Option[Note]): Either[(StatusCode, String), String] = {
      if (option.isDefined)
        Left(statusCode, "This ID is already exists!")
      notes.update(map => map + (note.id -> note))
      Right("Aded!")
    }

    notes.get.map(_.get(note.id)).map(logic)
  }

  def readNoteLogic(id: String): IO[Either[(StatusCode, String), Json]] = {
    def logic(option: Option[Note]): Either[(StatusCode, String), Json] = {
      if (option.isEmpty)
        Left(statusCode, "This ID is doesn't exists!")
      Right(option.get.asJson)
    }

    notes.get.map(_.get(id)).map(logic)
  }

  def updateNoteLogic(note: Note): IO[Either[(StatusCode, String), String]] = {
    def logic(option: Option[Note]):  Either[(StatusCode, String), String] = {
      if (option.isEmpty)
        Left(statusCode, "This ID is doesn't exists!")
      notes.update(map => map.updated(note.id, note))
      Right("Note was updated!")
    }

    notes.get.map(_.get(note.id)).map(logic)
  }

  def deleteNoteLogic(id: String): IO[Either[(StatusCode, String), String]] = {
    def logic(option: Option[Note]):  Either[(StatusCode, String), String] = {
      if (option.isEmpty)
        Left(statusCode, "This ID is doesn't exists!")
      notes.update(_.removed(id))
      Right("Note was removed!")
    }

    notes.get.map(_.get(id)).map(logic)
  }

  def listNoteLogic: IO[Either[(StatusCode, String), Json]] = {
    def logic(map: Map[String, Note]): Either[(StatusCode, String), Json] = {
      if (map.isEmpty)
        Left(statusCode, "Empty!")
      Right(map.toList.asJson)
    }

    notes.get.map(logic)
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
