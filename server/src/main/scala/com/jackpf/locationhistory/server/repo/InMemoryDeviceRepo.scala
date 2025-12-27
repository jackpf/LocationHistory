package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceAlreadyRegisteredException,
  DeviceNotFoundException
}
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class InMemoryDeviceRepo(using ec: ExecutionContext) extends DeviceRepo {
  private val storedDevices: mutable.Map[DeviceId.Type, StoredDevice] =
    mutable.Map.empty

  override def register(device: Device): Future[Try[Unit]] = Future.successful {
    if (!storedDevices.contains(device.id)) {
      val storedDevice =
        StoredDevice.fromDevice(device, status = DeviceStatus.Pending)
      storedDevices += (storedDevice.device.id -> storedDevice)
      Success(())
    } else {
      Failure(DeviceAlreadyRegisteredException(device.id))
    }
  }

  override def update(storedDevice: StoredDevice): Future[Try[Unit]] = Future.successful {
    if (storedDevices.contains(storedDevice.device.id)) {
      storedDevices += (storedDevice.device.id -> storedDevice)
      Success(())
    } else {
      Failure(DeviceNotFoundException(storedDevice.device.id))
    }
  }

  override def get(id: DeviceId.Type): Future[Option[StoredDevice]] =
    Future.successful {
      storedDevices.get(id)
    }

  override def getAll: Future[Seq[StoredDevice]] =
    Future.successful {
      storedDevices.values.toSeq
    }

  override def delete(id: DeviceId.Type): Future[Try[Unit]] = get(id).map {
    case Some(foundDevice) =>
      storedDevices -= foundDevice.device.id
      Success(())
    case None =>
      Failure(DeviceNotFoundException(id))
  }

  override def deleteAll(): Future[Unit] = Future.successful {
    storedDevices.clear()
  }
}
