package com.jackpf.locationhistory.server.db

import com.jackpf.locationhistory.server.model.StorageType
import org.sqlite.SQLiteDataSource
import scalasql.core.DbClient

class DataSourceFactory(dataDir: String, dbName: String) {
  def create(storageType: StorageType): Option[DbClient.DataSource] = storageType match {
    case StorageType.IN_MEMORY => None
    case StorageType.SQLITE    =>
      import scalasql.SqliteDialect.*

      val dataSource = new SQLiteDataSource()
      dataSource.setUrl(s"jdbc:sqlite:${dataDir}/${dbName}")
      Some(
        new DbClient.DataSource(
          dataSource,
          config = new scalasql.Config {}
        )
      )
  }
}
