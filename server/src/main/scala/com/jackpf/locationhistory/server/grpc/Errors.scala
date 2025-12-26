package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.server.model.DeviceId
import io.grpc.Status

object Errors {
  def noDeviceProvided(): Status = Status.INVALID_ARGUMENT
    .withDescription("No device provided")

  def deviceNotRegistered(deviceId: DeviceId.Type): Status =
    Status.INVALID_ARGUMENT
      .withDescription(s"Device ${deviceId} is not registered")

  def deviceAlreadyRegistered(deviceId: DeviceId.Type): Status =
    Status.INVALID_ARGUMENT
      .withDescription(s"Device ${deviceId} is already registered")
}
