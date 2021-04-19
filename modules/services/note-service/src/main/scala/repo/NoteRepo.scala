package repo

import cats.effect.IO
import models.Note

trait NoteRepo {

  def createNote(note: Note): IO[Either[String, String]]

  def readNote(noteID: String): IO[Either[String, Note]]

  def updateNote(note: Note): IO[Either[String, String]]

  def deleteNote(noteID: String): IO[Either[String, String]]

  def listNote(): IO[Either[String, List[Note]]]

}
