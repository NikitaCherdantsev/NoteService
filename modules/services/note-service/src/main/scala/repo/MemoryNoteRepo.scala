package repo

import cats.effect.IO
import cats.effect.concurrent.Ref
import models.Note

class MemoryNoteRepo(notes: Ref[IO, Map[String, Note]]) extends NoteRepo {

  override def createNote(note: Note): IO[Either[String, String]] =
    notes.modify[Either[String, String]] { map =>
      if (map.contains(note.id)) map -> Left("This note already exists")
      else map + (note.id            -> note) -> Right("Added")
    }

  override def readNote(noteID: String): IO[Either[String, Note]] =
    for {
      map <- notes.get
      result = map
        .get(noteID)
        .toRight[String]("This ID doesn't exist")
    } yield result

  override def updateNote(note: Note): IO[Either[String, String]] =
    notes.modify[Either[String, String]] { map =>
      if (map.contains(note.id)) map + (note.id -> note) -> Right("Note was updated")
      else map                                  -> Left("This note doesn't exist")
    }

  override def deleteNote(noteID: String): IO[Either[String, String]] =
    notes.modify[Either[String, String]] { map =>
      if (map.contains(noteID)) map - noteID -> Right("Note was removed")
      else map                               -> Left("This ID doesn't exist")
    }

  override def listNote(): IO[Either[String, List[Note]]] =
    for {
      map    <- notes.get
      result = Right(map.values.toList)
    } yield result

}
