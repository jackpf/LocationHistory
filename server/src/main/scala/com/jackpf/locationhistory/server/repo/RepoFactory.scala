package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.StorageType

class RepoFactory {
  def deviceRepo(storageType: StorageType): DeviceRepo = storageType match {
    case StorageType.IN_MEMORY => new InMemoryDeviceRepo
    case StorageType.SQLITE    => new SQLiteDeviceRepo
  }

  def locationRepo(storageType: StorageType): LocationRepo = storageType match {
    case StorageType.IN_MEMORY => new InMemoryLocationRepo
    case StorageType.SQLITE    => new SQLiteLocationRepo
  }
}
