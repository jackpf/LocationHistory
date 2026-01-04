package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.StorageType
import scalasql.core.DbClient

import scala.concurrent.ExecutionContext

class RepoFactory(db: Option[DbClient.DataSource])(using executionContext: ExecutionContext) {
  def deviceRepo(storageType: StorageType): DeviceRepo = storageType match {
    case StorageType.IN_MEMORY                             => new InMemoryDeviceRepo
    case StorageType.SQLITE | StorageType.SQLITE_IN_MEMORY => new SQLiteDeviceRepo(db.get)
  }

  def locationRepo(storageType: StorageType): LocationRepo = storageType match {
    case StorageType.IN_MEMORY                             => new InMemoryLocationRepo
    case StorageType.SQLITE | StorageType.SQLITE_IN_MEMORY => new SQLiteLocationRepo(db.get)
  }
}
