package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.server.model.DeviceId
import io.grpc.Status

object GrpcResponse {
  type GrpcResponse[T] = Either[Status, T]
  type GrpcFailure[T] = Left[Status, T]
  type GrpcSuccess[T] = Right[Status, T]

  def Failure[T](status: Status): GrpcResponse[T] = Left(status)

  def Success[T](value: T): GrpcResponse[T] = Right(value)
}

object Errors {
  def noDeviceProvided(): Status = Status.INVALID_ARGUMENT
    .withDescription("No device provided")

  def deviceAlreadyRegistered(deviceId: DeviceId.Type): Status =
    Status.INVALID_ARGUMENT
      .withDescription(s"Device ${deviceId} is already registered")
}
