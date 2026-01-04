package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.DeviceId.Type
import com.jackpf.locationhistory.server.model.{Device, StoredDevice}

import scala.concurrent.Future
import scala.util.Try

class SQLiteDeviceRepo extends DeviceRepo {
  override def register(device: Device): Future[Try[Unit]] = ???

  override def update(id: Type, updateAction: StoredDevice => StoredDevice): Future[Try[Unit]] = ???

  override def get(id: Type): Future[Option[StoredDevice]] = ???

  override def getAll: Future[Seq[StoredDevice]] = ???

  override def delete(id: Type): Future[Try[Unit]] = ???

  override def deleteAll(): Future[Unit] = ???
}
