package com.jackpf.locationhistory.server.errors

import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import io.grpc.Status

sealed abstract class ApplicationError(message: String, cause: Throwable)
    extends Exception(message, cause) {
  val status: Status
}

object ApplicationErrors {
  case class InvalidPassword() extends ApplicationError("Invalid password", None.orNull) {
    override val status: Status = Status.UNAUTHENTICATED
  }

  case class NoDeviceProvidedException(cause: Throwable = None.orNull)
      extends ApplicationError("No device provided", cause) {
    override val status: Status = Status.INVALID_ARGUMENT
  }

  case class NoLocationProvidedException(cause: Throwable = None.orNull)
      extends ApplicationError("No location provided", cause) {
    override val status: Status = Status.INVALID_ARGUMENT
  }

  case class DeviceNotRegisteredException(
      deviceId: DeviceId.Type,
      cause: Throwable = None.orNull
  ) extends ApplicationError(s"Device ${deviceId} is not registered", cause) {
    override val status: Status = Status.PERMISSION_DENIED
  }

  case class InvalidDeviceStatus(
      deviceId: DeviceId.Type,
      actualState: DeviceStatus,
      expectedState: DeviceStatus,
      cause: Throwable = None.orNull
  ) extends ApplicationError(
        s"Device ${deviceId} has an invalid state; expected ${expectedState} but was ${actualState}",
        cause
      ) {
    override val status: Status = Status.PERMISSION_DENIED
  }

  case class DeviceNotFoundException(
      deviceId: DeviceId.Type,
      cause: Throwable = None.orNull
  ) extends ApplicationError(s"Device ${deviceId} does not exist", cause) {
    override val status: Status = Status.NOT_FOUND
  }

  case class DeviceAlreadyRegisteredException(
      deviceId: DeviceId.Type,
      cause: Throwable = None.orNull
  ) extends ApplicationError(
        s"Device ${deviceId} is already registered",
        cause
      ) {
    override val status: Status = Status.ALREADY_EXISTS
  }
}
