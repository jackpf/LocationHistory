package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.LocationNotFoundException
import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.model.DeviceId.Type
import com.jackpf.locationhistory.server.model.{Location, StoredLocation}
import scalasql.core.DbClient
import scalasql.simple.SimpleTable
import scalasql.SqliteDialect.*

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.{Failure, Try}

private case class StoredLocationRow(
    id: Long,
    deviceId: String,
    lat: Double,
    lon: Double,
    accuracy: Double,
    timestamp: Long
) {
  def toStoredLocation: StoredLocation = StoredLocation(
    id = id,
    location = Location(lat = lat, lon = lon, accuracy = accuracy),
    timestamp = timestamp
  )
}
private object StoredLocationTable extends SimpleTable[StoredLocationRow]

class SQLiteLocationRepo(db: DbClient.DataSource)(using executionContext: ExecutionContext)
    extends LocationRepo {
  override def init(): Future[Unit] = Future {
    db.transaction { implicit db =>
      blocking {
        val _ = db.updateRaw(
          """CREATE TABLE IF NOT EXISTS stored_location_table (
            id INTEGER PRIMARY KEY, -- Auto-increment key
            device_id TEXT,
            lat DOUBLE,
            lon DOUBLE,
            accuracy DOUBLE,
            timestamp UNSIGNED BIG INT
          );"""
        )
        val _ = db.updateRaw(
          """CREATE INDEX IF NOT EXISTS idx_device_time ON stored_location_table (device_id, timestamp);"""
        )
      }
    }
  }

  override def storeDeviceLocation(
      deviceId: DeviceId.Type,
      location: Location,
      timestamp: Long
  ): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      Try {
        blocking {
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
  }

  override def getForDevice(
      deviceId: DeviceId.Type,
      limit: Option[Int]
  ): Future[Vector[StoredLocation]] = Future {
    db.transaction { implicit db =>
      blocking {
        db.run(
          {
            val q = StoredLocationTable.select
              .filter(_.deviceId === deviceId.toString)
              .sortBy(_.timestamp)
              .desc

            limit match {
              case Some(l) => q.take(l)
              case None    => q
            }
          }
        ).toVector
          .reverse // Reverse -> ascending order
          .map(_.toStoredLocation)
      }
    }
  }

  override def update(
      deviceId: Type,
      id: Long,
      updateAction: StoredLocation => StoredLocation
  ): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      /* This is not strictly race-condition-resistant
       * We can add a `version` field to StoredLocationRow if needed */
      val existingRowMaybe =
        blocking {
          db.run(
            StoredLocationTable.select.filter(l => l.id === id && l.deviceId === deviceId.toString)
          ).headOption
        }
      existingRowMaybe match {
        case Some(existingRow) =>
          val updatedStoredDevice = updateAction(existingRow.toStoredLocation)

          Try {
            blocking {
              db.run(
                StoredLocationTable
                  .update(l => l.id === id && l.deviceId === deviceId.toString)
                  .set(
                    _.lat := updatedStoredDevice.location.lat,
                    _.lon := updatedStoredDevice.location.lon,
                    _.accuracy := updatedStoredDevice.location.accuracy,
                    _.timestamp := updatedStoredDevice.timestamp
                  )
              )
            }
            ()
          }
        case None => Failure(LocationNotFoundException(deviceId, id))
      }
    }
  }

  override def deleteForDevice(deviceId: Type): Future[Unit] = Future {
    db.transaction { implicit db =>
      blocking {
        db.run(StoredLocationTable.delete(_.deviceId === deviceId.toString))
        ()
      }
    }
  }

  override def deleteAll(): Future[Unit] = Future {
    db.transaction { implicit db =>
      blocking {
        db.run(StoredLocationTable.delete(_ => true))
        ()
      }
    }
  }
}
