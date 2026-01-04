package com.jackpf.locationhistory.server.repo

import org.specs2.concurrent.ExecutionEnv
import com.jackpf.locationhistory.server.db.DataSourceFactory
import com.jackpf.locationhistory.server.model.StorageType

import java.util.UUID

class SQLiteDeviceRepoTest(implicit ee: ExecutionEnv) extends DeviceRepoTest {
  override def createDeviceRepo: DeviceRepo = {
    new SQLiteDeviceRepo(
      // TODO Fixme - in memory doesn't work properly
//      new DataSourceFactory(null, null).create(StorageType.SQLITE_IN_MEMORY).get
      new DataSourceFactory(System.getProperty("java.io.tmpdir"), s"tests_${UUID.randomUUID().toString}.db")
        .create(StorageType.SQLITE)
        .get
    )
  }
}
