package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceAlreadyRegisteredException,
  DeviceNotFoundException
}
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, PushHandler, StoredDevice}
import scalasql.SqliteDialect.*
import scalasql.core.DbClient
import scalasql.simple.SimpleTable
import org.sqlite.SQLiteErrorCode
import com.jackpf.locationhistory.server.util.SQLiteMapper.*

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.{Failure, Try}

private case class StoredDeviceRow(
    id: String,
    name: String,
    publicKey: String,
    status: String,
    pushHandlerName: String,
    pushHandlerUrl: String
) {
  def toStoredDevice: StoredDevice = StoredDevice(
    device = Device(id = DeviceId(id), name = name, publicKey = publicKey),
    status = DeviceStatus.valueOf(status),
    pushHandler =
      if (pushHandlerName != null)
        Some(
          PushHandler(
            name = pushHandlerName,
            url = pushHandlerUrl
          )
        )
      else None
  )
}
private object StoredDeviceTable extends SimpleTable[StoredDeviceRow]

class SQLiteDeviceRepo(db: DbClient.DataSource)(using executionContext: ExecutionContext)
    extends DeviceRepo {
  override def init(): Future[Unit] = Future {
    db.transaction { implicit db =>
      blocking {
        val _ = db.updateRaw(
          """CREATE TABLE IF NOT EXISTS stored_device_table (
            id TEXT PRIMARY KEY,
            name TEXT,
            public_key TEXT,
            status TEXT,
            push_handler_name TEXT,
            push_handler_url TEXT
          )"""
        )
      }
    }
  }

  override def register(device: Device): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      Try {
        blocking {
          db.run(
            StoredDeviceTable.insert.columns(
              _.id := device.id.toString,
              _.name := device.name,
              _.publicKey := device.publicKey,
              _.status := DeviceStatus.Pending.toString
            )
          )
        }
        ()
      }.mapErrors {
        case e if e.getResultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY =>
          DeviceAlreadyRegisteredException(device.id)
      }
    }
  }

  override def update(
      id: DeviceId.Type,
      updateAction: StoredDevice => StoredDevice
  ): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      /* This is not strictly race-condition-resistant
       * We can add a `version` field to StoredDeviceRow if needed */
      val existingRowMaybe =
        blocking { db.run(StoredDeviceTable.select.filter(_.id === id.toString)).headOption }
      existingRowMaybe match {
        case Some(existingRow) =>
          val updatedStoredDevice = updateAction(existingRow.toStoredDevice)

          Try {
            blocking {
              db.run(
                StoredDeviceTable
                  .update(_.id === id.toString)
                  .set(
                    _.name := updatedStoredDevice.device.name,
                    _.publicKey := updatedStoredDevice.device.publicKey,
                    _.status := updatedStoredDevice.status.toString,
                    _.pushHandlerName := updatedStoredDevice.pushHandler.map(_.name).orNull,
                    _.pushHandlerUrl := updatedStoredDevice.pushHandler.map(_.url).orNull
                  )
              )
            }
            ()
          }
        case None => Failure(DeviceNotFoundException(id))
      }
    }
  }

  override def get(id: DeviceId.Type): Future[Option[StoredDevice]] = Future {
    db.transaction { implicit db =>
      blocking {
        db.run(StoredDeviceTable.select.filter(_.id === id.toString))
          .headOption
          .map(_.toStoredDevice)
      }
    }
  }

  override def getAll: Future[Seq[StoredDevice]] = Future {
    db.transaction { implicit db =>
      blocking { db.run(StoredDeviceTable.select.filter(_ => true)).map(_.toStoredDevice) }
    }
  }

  override def delete(id: DeviceId.Type): Future[Try[Unit]] = Future {
    db.transaction { implicit db =>
      Try {
        val result = blocking { db.run(StoredDeviceTable.delete(_.id === id.toString)) }
        if (result == 0) throw DeviceNotFoundException(id)
        else ()
      }
    }
  }

  override def deleteAll(): Future[Unit] = Future {
    db.transaction { implicit db =>
      blocking { db.run(StoredDeviceTable.delete(_ => true)) }
      ()
    }
  }
}
