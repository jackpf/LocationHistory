package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.model.StorageType

import java.nio.file.{Path, Paths}

case class Args(
    beaconPort: Option[Int] = None,
    adminPort: Option[Int] = None,
    adminPassword: Option[String] = None,
    dataDirectory: Option[String] = None,
    storageType: Option[StorageType] = None,
    sslCertsDir: Option[String] = None,
    enrichers: Seq[String] = Seq.empty
) {
  def sslCertsPath: Option[Path] = sslCertsDir.map(Paths.get(_))
}
