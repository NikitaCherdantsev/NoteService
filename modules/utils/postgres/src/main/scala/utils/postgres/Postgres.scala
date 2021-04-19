package utils.postgres

import java.sql.DriverManager

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.getquill.context.jdbc.JdbcContextBase
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import utils.postgres.model.DBConfig

object Postgres {

  def createTransactor[F[_]: Async: ContextShift](
      dbConfig: DBConfig,
      path: String,
      changelogFile: String,
      databaseDriver: String
  ): Resource[F, HikariTransactor[F]] =
    for {
      connectionThreads  <- ExecutionContexts.fixedThreadPool(dbConfig.databaseThreadSize)
      transactionThreads <- ExecutionContexts.cachedThreadPool
      transactor <- HikariTransactor.newHikariTransactor[F](
                     databaseDriver,
                     dbConfig.databaseUri,
                     dbConfig.databaseUsername,
                     dbConfig.databasePassword,
                     connectionThreads,
                     Blocker.liftExecutionContext(transactionThreads)
                   )
      _ <- Resource.liftF {
            transactor.configure { ds =>
              Sync[F].delay {
                ds.setPoolName(path)
                ds.setRegisterMbeans(true)
              }
            }
          }

      _ <- Resource.liftF {
            Sync[F].delay {
              val liquibase = new Liquibase(
                changelogFile,
                new ClassLoaderResourceAccessor(),
                DatabaseFactory
                  .getInstance()
                  .findCorrectDatabaseImplementation(
                    new JdbcConnection(
                      DriverManager
                        .getConnection(dbConfig.databaseUri, dbConfig.databaseUsername, dbConfig.databasePassword)
                    )
                  )
              )
              liquibase.update("main")
            }
          }

    } yield transactor

  def jsonbDecoder[T: Decoder](context: JdbcContextBase[_, _]): context.Decoder[T] =
    context.decoder((index, row) => decode[T](row.getObject(index).toString).toOption.get)

  def jsonbEncoder[T: Encoder](context: JdbcContextBase[_, _]): context.Encoder[T] =
    context.encoder(
      java.sql.Types.OTHER,
      (index, value, row) => row.setObject(index, value.asJson.noSpaces, java.sql.Types.OTHER)
    )
}
