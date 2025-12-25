package com.jackpf.locationhistory.server.model

import beacon.beacon_service.Device as ProtoDevice

object Device {
  def fromProto(proto: ProtoDevice): Device = Device(
    id = proto.deviceId,
    publicKey = proto.publicKey
  )
}

case class Device(id: String, publicKey: String)
