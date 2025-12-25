package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, StoredDevice}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class InMemoryDeviceRepo extends DeviceRepo {
  private val storedDevices: mutable.ArrayBuffer[StoredDevice] =
    ArrayBuffer().empty

  override def register(device: Device): Future[Unit] =
    Future.successful {
      val storedDevice = StoredDevice.fromDevice(device)
      storedDevices += storedDevice
    }

  override def getById(id: String): Future[Option[StoredDevice]] =
    Future.successful {
      storedDevices.find(_.device.id == id)
    }

  override def get(device: Device): Future[Option[StoredDevice]] =
    Future.successful {
      storedDevices.find(_ == device)
    }

  override def getAll: Future[Seq[StoredDevice]] =
    Future.successful {
      storedDevices.toSeq
    }
}
