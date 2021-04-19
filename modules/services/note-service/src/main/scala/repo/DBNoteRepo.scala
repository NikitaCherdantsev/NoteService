package repo


import cats.effect.{ContextShift, IO, Resource, Sync}
import doobie.implicits._
import doobie.quill.DoobieContext
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.Logger
import io.getquill.SnakeCase
import models.Note
import utils.logging.Logging._
import utils.postgres.Postgres._
import utils.postgres.model.DBConfig


class DBNoteRepo(transactor: Transactor[IO])(
    implicit val contextShift: ContextShift[IO],
    implicit val logger: Logger[IO]
) extends NoteRepo {

  private val doobieContext = new DoobieContext.Postgres(SnakeCase)
  import doobieContext._
  private val notes = quote(querySchema[Note]("notes"))

  override def createNote(note: Note): IO[Either[String, String]] = {
    val q = run(quote(notes.insert(lift(note))))
    for {
      _ <- logStarting("creating note")
      result <- q.transact(transactor).guaranteeCase(logExitCase("creating note")).attempt.map {
                 case Right(_) => Right("Added")
                 case Left(_)  => Left("This note already exists")
               }
    } yield result

  }

  override def readNote(noteID: String): IO[Either[String, Note]] = {
    val q = run(quote(notes.filter(_.id == lift(noteID)))).map(_.head)
    for {
      _ <- logStarting("reading note")
      result <- q.transact(transactor).guaranteeCase(logExitCase("reading note")).attempt.map {
                 case Left(_)      => Left("This note already exists")
                 case Right(value) => Right(value)
               }
    } yield result

  }

  override def updateNote(note: Note): IO[Either[String, String]] = {
    val q = run(quote(notes.filter(_.id == lift(note.id)).update(lift(Note(note.id, note.title, note.text)))))
    for {
      _            <- logStarting("updating note")
      existingNote <- readNote(note.id)
      result <- q.transact(transactor).guaranteeCase(logExitCase("updating note")).attempt.map {
                 case Right(_) if existingNote.isRight => Right("Note was updated")
                 case _                                => Left("This note doesn't exist")
               }
    } yield result
  }

  override def deleteNote(noteID: String): IO[Either[String, String]] = {
    val q = run(quote(notes.filter(_.id == lift(noteID)).delete))
    for {
      _            <- logger.info("deleting note")
      existingNote <- readNote(noteID)
      result <- q.transact(transactor).guaranteeCase(logExitCase("deleting note")).attempt.map {
                 case Right(_) if existingNote.isRight => Right("Note was removed")
                 case _                                => Left("This ID doesn't exist")
               }
    } yield result
  }

  override def listNote(): IO[Either[String, List[Note]]] = {
    val q = run(quote(notes))
    for {
      _ <- logStarting("list note")
      result <- q.transact(transactor).guaranteeCase(logExitCase("list note")).map(_.toList).attempt.map {
                 case Left(_)      => Left("Empty")
                 case Right(value) => Right(value)
               }
    } yield result

  }

}

object DBNoteRepo {
  private val DATABASE_DRIVER = "org.postgresql.Driver"
  private val CHANGELOG_FILE = "DBNoteRepoChangelog.xml"

  def apply(
      config: DBConfig,
      path: String = "keys-service"
  )(
      implicit cs: ContextShift[IO],
      logger: Logger[IO]
  ): Resource[IO, DBNoteRepo] =
    createTransactor[IO](config, path, CHANGELOG_FILE, DATABASE_DRIVER).map(new DBNoteRepo(_))

}
