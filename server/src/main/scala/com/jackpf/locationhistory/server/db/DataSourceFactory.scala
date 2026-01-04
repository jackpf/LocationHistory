package com.jackpf.locationhistory.server.db

import com.jackpf.locationhistory.server.model.StorageType
import org.sqlite.SQLiteDataSource
import scalasql.core.DbClient

import java.nio.file.Paths

class DataSourceFactory(dataDir: String, dbName: String) {
  private def newSQLite(
      connectionString: String,
      config: scalasql.Config = new scalasql.Config {}
  ): DbClient.DataSource = {
    import scalasql.SqliteDialect.*

    val dataSource = new SQLiteDataSource()
    dataSource.setUrl(s"jdbc:sqlite:${connectionString}")
    new DbClient.DataSource(dataSource, config)
  }

  def create(storageType: StorageType): Option[DbClient.DataSource] = storageType match {
    case StorageType.IN_MEMORY        => None
    case StorageType.SQLITE_IN_MEMORY => Some(newSQLite(":memory:"))
    case StorageType.SQLITE           => Some(newSQLite(Paths.get(dataDir, dbName).toString))
  }
}
