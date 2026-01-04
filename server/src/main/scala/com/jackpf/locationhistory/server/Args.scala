package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.model.StorageType

case class Args(
    listenPort: Option[Int] = None,
    adminPassword: Option[String] = None,
    dataDirectory: Option[String] = None,
    storageType: Option[StorageType] = None
)
