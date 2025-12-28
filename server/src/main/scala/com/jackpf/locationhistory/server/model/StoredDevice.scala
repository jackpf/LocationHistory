package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.DeviceStatus as ProtoDeviceStatus
import com.jackpf.locationhistory.common.StoredDevice as ProtoStoredDevice

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

case class StoredDevice(device: Device, status: StoredDevice.DeviceStatus) {
  def register(): StoredDevice = copy(status = StoredDevice.DeviceStatus.Registered)

  def toProto: ProtoStoredDevice = ProtoStoredDevice(
    device = Some(device.toProto),
    status = status.toProto
  )
}
