package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.Device as ProtoDevice

object DeviceId {
  opaque type Type = String
  def apply(value: String): Type = value
  def value(id: Type): String = id
}

object Device {
  def fromProto(device: ProtoDevice): Device = Device(
    id = DeviceId(device.id),
    name = device.name
  )
}

case class Device(id: DeviceId.Type, name: String) {
  def toProto: ProtoDevice = ProtoDevice(
    id = id.toString,
    name = name
  )
}
