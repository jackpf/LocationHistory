package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.StorageType
import scalasql.core.DbClient

import scala.concurrent.ExecutionContext

class RepoFactory(dataSource: Option[DbClient.DataSource])(using
    executionContext: ExecutionContext
) {
  private def getDataSource(storageType: StorageType): DbClient.DataSource =
    dataSource.getOrElse(
      throw new RuntimeException(s"Storage type ${storageType} requires a data source")
    )

  def deviceRepo(storageType: StorageType): DeviceRepo = storageType match {
    case StorageType.IN_MEMORY                             => new InMemoryDeviceRepo
    case StorageType.SQLITE | StorageType.SQLITE_IN_MEMORY =>
      new SQLiteDeviceRepo(getDataSource(storageType))
  }

  def locationRepo(storageType: StorageType): LocationRepo = storageType match {
    case StorageType.IN_MEMORY                             => new InMemoryLocationRepo
    case StorageType.SQLITE | StorageType.SQLITE_IN_MEMORY =>
      new SQLiteLocationRepo(getDataSource(storageType))
  }
}
