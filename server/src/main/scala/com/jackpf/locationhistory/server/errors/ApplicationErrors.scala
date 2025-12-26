package com.jackpf.locationhistory.server.errors

import com.jackpf.locationhistory.server.model.DeviceId
import io.grpc.Status

sealed abstract class ApplicationError(message: String, cause: Throwable)
    extends Exception(message, cause) {
  val status: Status
}

object ApplicationErrors {
  case class NoDeviceProvidedException(cause: Throwable = None.orNull)
      extends ApplicationError("No device provided", cause) {
    override val status: Status = Status.INVALID_ARGUMENT
  }

  case class DeviceNotRegisteredException(
      deviceId: DeviceId.Type,
      cause: Throwable = None.orNull
  ) extends ApplicationError(s"Device ${deviceId} is not registered", cause) {
    override val status: Status = Status.INVALID_ARGUMENT
  }

  case class DeviceAlreadyRegisteredException(
      deviceId: DeviceId.Type,
      cause: Throwable = None.orNull
  ) extends ApplicationError(
        s"Device ${deviceId} is already registered",
        cause
      ) {
    override val status: Status = Status.INVALID_ARGUMENT
  }
}
