import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class Note(id: String, title: String, text: String)

object Note {
  implicit val serviceInfoReader: Codec[Note] = deriveCodec
}
