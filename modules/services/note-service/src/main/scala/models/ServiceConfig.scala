package models

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import utils.postgres.model.DBConfig

case class ServiceConfig(
    host: String,
    port: Int,
    database: DBConfig
)

object ServiceConfig {
  implicit val configReader: ConfigReader[ServiceConfig] = deriveReader
}
