import cats.implicits._
import cats.effect._, concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


case class Note(id: String, title: String, text: String)