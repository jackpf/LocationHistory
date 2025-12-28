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
    publicKey = device.publicKey
  )
}

case class Device(id: DeviceId.Type, publicKey: String) {
  def toProto: ProtoDevice = ProtoDevice(
    id = id.toString,
    publicKey = publicKey
  )
}
