package com.jackpf.locationhistory.server.testutil

import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, PushHandler, StoredDevice}

object MockModels {
  def device(
      id: DeviceId.Type = DeviceId("dev1"),
      name: String = "device 1 name",
      publicKey: String = "xxx"
  ): Device = Device(id = id, name = name, publicKey = publicKey)

  def pushHandler(
      name: String = "phName",
      url: String = "phUrl"
  ): PushHandler = PushHandler(name = name, url = url)

  def storedDevice(
      device: Device = device(),
      status: DeviceStatus = DeviceStatus.Unknown,
      pushHandler: Option[PushHandler] = None
  ): StoredDevice = StoredDevice(device = device, status = status, pushHandler = pushHandler)
}
