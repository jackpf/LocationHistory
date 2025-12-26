package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.server.errors.ApplicationError
import io.grpc.Status

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
      case other =>
        Status.INTERNAL.withDescription(other.getMessage).withCause(other)
    }

    def toGrpcError: Throwable = toGrpcStatus.asException()
  }
}
