package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.model.{Location, StoredLocation}
import scalasql.core.DbClient
import scalasql.simple.SimpleTable
import scalasql.SqliteDialect.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

private case class StoredLocationRow(
    id: Long,
    deviceId: String,
    lat: Double,
    lon: Double,
    accuracy: Double,
    timestamp: Long
) {
  def toStoredLocation: StoredLocation = StoredLocation(
    location = Location(lat = lat, lon = lon, accuracy = accuracy),
    timestamp = timestamp
  )
}
private object StoredLocationTable extends SimpleTable[StoredLocationRow]

class SQLiteLocationRepo(db: DbClient.DataSource)(using executionContext: ExecutionContext)
    extends LocationRepo {
  override def init(): Future[Unit] = Future {
    db.transaction { implicit db =>
      val _ = db.updateRaw(
        """CREATE TABLE IF NOT EXISTS stored_location_table (
          id UNSIGNED BIG INT PRIMARY KEY,
          device_id TEXT,
          lat DOUBLE,
          lon DOUBLE,
          accuracy DOUBLE,
          timestamp UNSIGNED BIG INT
       )"""
      )
    }
  }

  override def storeDeviceLocation(
      deviceId: DeviceId.Type,
      location: Location,
      timestamp: Long
  ): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      Try {
        db.run(
          StoredLocationTable.insert.columns(
            _.deviceId := deviceId.toString,
            _.lat := location.lat,
            _.lon := location.lon,
            _.accuracy := location.accuracy,
            _.timestamp := timestamp
          )
        )
        ()
      }
    }
  }

  override def getForDevice(deviceId: DeviceId.Type): Future[Vector[StoredLocation]] = Future {
    db.transaction { implicit db =>
      db.run(StoredLocationTable.select.filter(_.deviceId === deviceId.toString))
        .toVector
        .map(_.toStoredLocation)
    }
  }

  override def deleteAll(): Future[Unit] = Future {
    db.transaction { implicit db =>
      db.run(StoredLocationTable.delete(_ => true))
      ()
    }
  }
}
