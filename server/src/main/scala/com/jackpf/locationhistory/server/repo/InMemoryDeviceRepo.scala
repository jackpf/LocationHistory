package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceAlreadyRegisteredException,
  DeviceNotFoundException
}
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}

import scala.collection.concurrent
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class InMemoryDeviceRepo extends DeviceRepo {
  private val storedDevices: concurrent.Map[DeviceId.Type, StoredDevice] =
    concurrent.TrieMap.empty

  override def register(device: Device): Future[Try[Unit]] = Future.successful {
    val storedDevice =
      StoredDevice.fromDevice(device, status = DeviceStatus.Pending)
    val result = storedDevices.putIfAbsent(storedDevice.device.id, storedDevice)

    result match {
      case None    => Success(())
      case Some(_) => Failure(DeviceAlreadyRegisteredException(device.id))
    }
  }

  override def update(
      id: DeviceId.Type,
      updateAction: StoredDevice => StoredDevice
  ): Future[Try[Unit]] = Future.successful {
    val result = storedDevices.updateWith(id) {
      case Some(foundDevice) => Some(updateAction(foundDevice))
      case None              => None
    }

    result match {
      case Some(_) => Success(())
      case None    => Failure(DeviceNotFoundException(id))
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

  override def delete(id: DeviceId.Type): Future[Try[Unit]] = Future.successful {
    storedDevices.remove(id) match {
      case Some(_) => Success(())
      case None    => Failure(DeviceNotFoundException(id))
    }
  }

  override def deleteAll(): Future[Unit] = Future.successful {
    storedDevices.clear()
  }
}
