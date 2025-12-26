package com.jackpf.locationhistory.server.util

import io.grpc.Status

object GrpcResponse {
  opaque type GrpcTry[+T] = Either[Status, T]
  // TODO make work or remove
  type GrpcFailure[T] = Left[Status, T]
  type GrpcSuccess[T] = Right[Status, T]

  object GrpcFailure {
    def unapply[T](e: Either[Status, T]): Option[Status] = e.left.toOption
  }

  object GrpcSuccess {
    def unapply[T](e: Either[Status, T]): Option[T] = e.toOption
  }

  def Failure[T](status: Status): GrpcTry[T] = Left(status)

  def Success[T](value: T): GrpcTry[T] = Right(value)
}
