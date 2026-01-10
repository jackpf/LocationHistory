package com.jackpf.locationhistory.server.util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object ParamExtractor {
  extension [T](param: Option[T]) {
    def toTryOr(failure: Throwable): Try[T] = param match {
      case Some(value) => Success(value)
      case None        => Failure(failure)
    }

    def toFutureOr(failure: Throwable): Future[T] = param match {
      case Some(value) => Future.successful(value)
      case None        => Future.failed(failure)
    }
  }

  extension [T](param: Try[T]) {
    def toFuture: Future[T] = param match {
      case Success(value)     => Future.successful(value)
      case Failure(exception) => Future.failed(exception)
    }
  }

  extension [T](param: Future[Option[T]]) {
    def toFutureOr(failure: Throwable)(using ec: ExecutionContext): Future[T] = param.flatMap {
      case Some(value) => Future.successful(value)
      case None        => Future.failed(failure)
    }
  }
}
