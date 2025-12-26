package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.grpc.GrpcResponse.GrpcResponse
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}

import scala.concurrent.Future

trait DeviceRepo {
  def register(device: Device): Future[GrpcResponse[Unit]]

  def get(id: DeviceId.Type): Future[Option[StoredDevice]]

  def getAll: Future[Seq[StoredDevice]]
}
