package com.jackpf.locationhistory.server.util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.jackpf.locationhistory.server.grpc.ErrorMapper.*

object ResponseMapper {
  extension [T](response: Future[Try[T]]) {
    def toResponse[R](f: T => R)(using ec: ExecutionContext): Future[R] = response
      .flatMap(t => Future.fromTry(t.map(f)))
      .recoverWith { case t: Throwable =>
        Future.failed(t.toGrpcError)
      }
  }
}
