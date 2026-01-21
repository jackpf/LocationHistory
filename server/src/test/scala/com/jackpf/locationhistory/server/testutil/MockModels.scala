package com.jackpf.locationhistory.server.testutil

import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{
  Device,
  DeviceId,
  Location,
  PushHandler,
  StoredDevice,
  StoredLocation
}

object MockModels {
  def device(
      id: DeviceId.Type = DeviceId("dev1"),
      name: String = "device 1 name"
  ): Device = Device(id = id, name = name)

  def pushHandler(
      name: String = "phName",
      url: String = "phUrl"
  ): PushHandler = PushHandler(name = name, url = url)

  def storedDevice(
      device: Device = device(),
      status: DeviceStatus = DeviceStatus.Unknown,
      pushHandler: Option[PushHandler] = None
  ): StoredDevice = StoredDevice(device = device, status = status, pushHandler = pushHandler)

  def location(
      lat: Double = 0.1,
      lon: Double = 0.2,
      accuracy: Double = 0.3,
      metadata: Map[String, String] = Map("key1" -> "value1", "key2" -> "value2")
  ): Location =
    Location(lat = lat, lon = lon, accuracy = accuracy, metadata = metadata)

  def storedLocation(
      id: Long = 1,
      location: Location = location(),
      timestamp: Long = 123L
  ): StoredLocation = StoredLocation(id = id, location = location, timestamp = timestamp)
}
