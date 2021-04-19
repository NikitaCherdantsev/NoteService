package utils.logging

import cats.effect.{ExitCase, IO}
import io.chrisdavenport.log4cats.Logger

object Logging {

  def logStarting(name: String)(implicit logger: Logger[IO]): IO[Unit] = logger.info(s"Start $name")

  def logExitCase(name: String)(exitCase: ExitCase[Throwable])(implicit logger: Logger[IO]): IO[Unit] = exitCase match {
    case ExitCase.Completed => logger.info(s"Success: $name")
    case ExitCase.Error(e)  => logger.warn(s"Failure: $name: $e")
    case ExitCase.Canceled  => logger.warn(s"Canceled: $name")
  }
}
