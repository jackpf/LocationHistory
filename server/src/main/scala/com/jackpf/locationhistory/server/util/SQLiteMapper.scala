package com.jackpf.locationhistory.server.util

import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.parser.decode
import io.circe.syntax.*
import org.sqlite.SQLiteException
import scalasql.core.TypeMapper

import java.sql.{JDBCType, PreparedStatement, ResultSet}
import scala.util.{Failure, Success, Try}

object SQLiteMapper {

  /** Generic wrapped JSON column
    */
  case class JsonColumn[T](value: T)

  /** Maps supported (by Circe) types to and from JSON
    */
  implicit def jsonTypeMapper[T](using
      enc: Encoder[T],
      dec: Decoder[T]
  ): TypeMapper[JsonColumn[T]] = new TypeMapper[JsonColumn[T]] {
    override def jdbcType = JDBCType.VARCHAR

    override def put(
        stmt: PreparedStatement,
        idx: Int,
        wrapped: JsonColumn[T]
    ): Unit = {
      stmt.setString(idx, wrapped.value.asJson.toString)
    }

    override def get(
        resultSet: ResultSet,
        idx: Int
    ): JsonColumn[T] = {
      val jsonStr = resultSet.getString(idx)
      if (jsonStr == null || jsonStr.isEmpty) throw DecodingFailure("Empty value", List.empty)
      else
        decode[T](jsonStr) match {
          case Left(error)  => throw error
          case Right(value) => JsonColumn(value)
        }
    }
  }

  extension [T](t: Try[T]) {

    /** Enables mapping of certain SQLite errors to application errors, e.g.
      * mapErrors {
      *  case e if e.getResultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY =>
      *    DeviceAlreadyRegisteredException(device.id)
      * }
      */
    def mapErrors(mapper: PartialFunction[SQLiteException, Throwable]): Try[T] = {
      t.transform(
        value => Success(value),
        {
          case sqliteException: SQLiteException =>
            Failure(mapper.applyOrElse(sqliteException, identity))
          case other => Failure(other)
        }
      )
    }
  }
}
