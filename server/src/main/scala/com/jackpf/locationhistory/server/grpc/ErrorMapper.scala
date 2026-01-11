package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.server.errors.ApplicationError
import io.grpc.{Status, StatusException, StatusRuntimeException}

import scala.concurrent.ExecutionException

object ErrorMapper {
  private def applicationErrorToStatus(
      applicationError: ApplicationError
  ): Status = applicationError.status
    .withDescription(applicationError.getMessage)
    .withCause(applicationError)

  extension (throwable: Throwable) {
    def toGrpcStatus: Status = throwable match {
      case applicationError: ApplicationError =>
        applicationErrorToStatus(applicationError)
      case boxed: ExecutionException if boxed.getCause != null =>
        Status.INTERNAL.withDescription(boxed.getCause.getMessage).withCause(boxed.getCause)
      case other =>
        Status.INTERNAL.withDescription(other.getMessage).withCause(other)
    }

    def toGrpcError: Throwable = if (
      throwable.isInstanceOf[StatusException] || throwable.isInstanceOf[StatusRuntimeException]
    ) throwable
    else toGrpcStatus.asException()
  }
}
