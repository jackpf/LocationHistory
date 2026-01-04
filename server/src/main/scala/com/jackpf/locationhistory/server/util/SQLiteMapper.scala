package com.jackpf.locationhistory.server.util

import org.sqlite.SQLiteException

import scala.util.{Failure, Success, Try}

object SQLiteMapper {
  extension [T](t: Try[T]) {
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
