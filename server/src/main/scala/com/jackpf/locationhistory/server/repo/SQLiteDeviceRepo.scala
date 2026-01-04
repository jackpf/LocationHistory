package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceAlreadyRegisteredException,
  DeviceNotFoundException
}
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}
import scalasql.SqliteDialect.*
import scalasql.core.DbClient
import scalasql.simple.SimpleTable

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

private case class StoredDeviceRow(id: String, publicKey: String, status: String) {
  def toStoredDevice: StoredDevice = StoredDevice(
    device = Device(id = DeviceId(id), publicKey = publicKey),
    status = DeviceStatus.valueOf(status)
  )
}
private object StoredDeviceTable extends SimpleTable[StoredDeviceRow]

class SQLiteDeviceRepo(db: DbClient.DataSource)(using executionContext: ExecutionContext)
    extends DeviceRepo {
  override def init(): Future[Unit] = Future {
    db.transaction { implicit db =>
      val _ = db.updateRaw(
        """CREATE TABLE IF NOT EXISTS stored_device_table (
          id TEXT PRIMARY KEY,
          public_key TEXT,
          status TEXT
       )"""
      )
    }
  }

  override def register(device: Device): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      val existingRowMaybe =
        db.run(StoredDeviceTable.select.filter(_.id === device.id.toString)).headOption

      existingRowMaybe match {
        case None =>
          Try {
            db.run(
              StoredDeviceTable.insert.columns(
                _.id := device.id.toString,
                _.publicKey := device.publicKey,
                _.status := DeviceStatus.Pending.toString
              )
            )
            ()
          }
        case Some(existingRow) =>
          Failure(DeviceAlreadyRegisteredException(DeviceId(existingRow.id)))
      }
    }
  }

  override def update(
      id: DeviceId.Type,
      updateAction: StoredDevice => StoredDevice
  ): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      val existingRowMaybe =
        db.run(StoredDeviceTable.select.filter(_.id === id.toString)).headOption
      existingRowMaybe match {
        case Some(existingRow) =>
          val updatedStoredDevice = updateAction(existingRow.toStoredDevice)

          Try {
            db.run(
              StoredDeviceTable
                .update(_.id === id.toString)
                .set(
                  _.id := updatedStoredDevice.device.id.toString,
                  _.publicKey := updatedStoredDevice.device.publicKey,
                  _.status := updatedStoredDevice.status.toString
                )
            )
            ()
          }
        case None => Failure(DeviceNotFoundException(id))
      }
    }
  }

  override def get(id: DeviceId.Type): Future[Option[StoredDevice]] = Future {
    db.transaction { implicit db =>
      db.run(StoredDeviceTable.select.filter(_.id === id.toString)).headOption.map(_.toStoredDevice)
    }
  }

  override def getAll: Future[Seq[StoredDevice]] = Future {
    db.transaction { implicit db =>
      db.run(StoredDeviceTable.select.filter(_ => true)).map(_.toStoredDevice)
    }
  }

  override def delete(id: DeviceId.Type): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      val existingRowMaybe =
        db.run(StoredDeviceTable.select.filter(_.id === id.toString)).headOption

      existingRowMaybe match {
        case Some(existingDevice) =>
          Try {
            db.run(StoredDeviceTable.delete(_.id === existingDevice.id))
            ()
          }
        case None =>
          Failure(DeviceNotFoundException(id))
      }
    }
  }

  override def deleteAll(): Future[Unit] = Future {
    db.transaction { implicit db =>
      db.run(StoredDeviceTable.delete(_ => true))
      ()
    }
  }
}
