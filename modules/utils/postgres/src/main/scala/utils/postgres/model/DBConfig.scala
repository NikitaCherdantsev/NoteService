package utils.postgres.model

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

case class DBConfig(
    databaseUri: String,
    databaseUsername: String,
    databasePassword: String,
    databaseThreadSize: Int
)

object DBConfig {
  implicit val configReader: ConfigReader[DBConfig] = deriveReader
}
