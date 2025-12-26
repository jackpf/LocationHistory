package com.jackpf.locationhistory.server.model

import beacon.beacon_service.DeviceStatus as ProtoDeviceStatus

object StoredDevice {
  enum DeviceStatus {
    case Unknown, Pending, Registered

    def toProto: ProtoDeviceStatus = this match {
      case DeviceStatus.Unknown    => ProtoDeviceStatus.DEVICE_UNKNOWN
      case DeviceStatus.Pending    => ProtoDeviceStatus.DEVICE_PENDING
      case DeviceStatus.Registered => ProtoDeviceStatus.DEVICE_REGISTERED
    }
  }

  def fromDevice(
      device: Device,
      status: DeviceStatus = DeviceStatus.Unknown
  ): StoredDevice = StoredDevice(
    device = device,
    status = status
  )
}

case class StoredDevice(device: Device, status: StoredDevice.DeviceStatus)
