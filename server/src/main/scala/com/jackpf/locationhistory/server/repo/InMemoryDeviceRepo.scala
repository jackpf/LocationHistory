package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class InMemoryDeviceRepo(using ec: ExecutionContext) extends DeviceRepo {
  private val storedDevices: mutable.Map[DeviceId.Type, StoredDevice] =
    mutable.Map.empty

  override def register(device: Device): Future[Try[Unit]] =
    get(device.id).map {
      case Some(_) =>
        Failure[Unit](
          new IllegalArgumentException(
            s"Device ${device.id} is already registered"
          )
        )
      case None =>
        val storedDevice =
          StoredDevice.fromDevice(device, status = DeviceStatus.Pending)
        storedDevices += (storedDevice.device.id -> storedDevice)
        Success[Unit](())
    }

  override def get(id: DeviceId.Type): Future[Option[StoredDevice]] =
    Future.successful {
      storedDevices.get(id)
    }

  override def getAll: Future[Seq[StoredDevice]] =
    Future.successful {
      storedDevices.values.toSeq
    }
}
