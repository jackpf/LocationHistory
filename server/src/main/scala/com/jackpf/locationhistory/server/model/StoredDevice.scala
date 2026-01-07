package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.{
  DeviceStatus as ProtoDeviceStatus,
  StoredDevice as ProtoStoredDevice
}

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
      status: DeviceStatus = DeviceStatus.Unknown,
      pushHandler: Option[PushHandler] = None
  ): StoredDevice = StoredDevice(
    device = device,
    status = status,
    pushHandler = pushHandler
  )
}

case class StoredDevice(
    device: Device,
    status: StoredDevice.DeviceStatus,
    pushHandler: Option[PushHandler]
) {
  def register(): StoredDevice = copy(status = StoredDevice.DeviceStatus.Registered)

  def withPushHandler(pushHandler: Option[PushHandler]): StoredDevice = copy(
    pushHandler = pushHandler
  )

  def toProto: ProtoStoredDevice = ProtoStoredDevice(
    device = Some(device.toProto),
    status = status.toProto,
    pushHandler = pushHandler.map(_.toProto)
  )
}
