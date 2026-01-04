package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.db.DataSourceFactory
import com.jackpf.locationhistory.server.model.StorageType
import org.specs2.concurrent.ExecutionEnv

import java.util.UUID

class SQLiteLocationRepoTest(implicit ee: ExecutionEnv) extends LocationRepoTest {
  override def createLocationRepo: LocationRepo = {
    new SQLiteLocationRepo(
      // TODO Fixme - in memory doesn't work properly
//      new DataSourceFactory(null, null)
//        .create(StorageType.SQLITE_IN_MEMORY)
//        .get
      new DataSourceFactory(
        System.getProperty("java.io.tmpdir"),
        s"tests_${UUID.randomUUID().toString}.db"
      )
        .create(StorageType.SQLITE)
        .get
    )
  }
}
