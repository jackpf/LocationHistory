package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.grpc.Errors
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Location, StoredDevice}
import com.jackpf.locationhistory.server.util.GrpcResponse.{
  Failure,
  GrpcTry,
  Success
}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class InMemoryLocationRepo extends LocationRepo {
  private val storedLocations: mutable.ArrayBuffer[(StoredDevice, Location)] =
    ArrayBuffer().empty

  override def storeDeviceLocation(
      storedDevice: StoredDevice,
      location: Location
  ): Future[GrpcTry[Unit]] =
    Future.successful {
      if (storedDevice.status == DeviceStatus.Registered) {
        storedLocations += ((storedDevice, location))
        Success[Unit](())
      } else {
        Failure(Errors.deviceNotRegistered(storedDevice.device.id))
      }
    }
}
