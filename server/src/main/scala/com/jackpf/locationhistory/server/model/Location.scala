package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.Location as ProtoLocation

object Location {
  def fromProto(
      proto: ProtoLocation,
      timestamp: Long
  ): Location = Location(
    lat = proto.lat,
    lon = proto.lon,
    accuracy = proto.accuracy,
    timestamp = timestamp
  )
}

case class Location(lat: Double, lon: Double, accuracy: Double, timestamp: Long) {
  def toProto: ProtoLocation = ProtoLocation(
    lat = lat,
    lon = lon,
    accuracy = accuracy
  )
}
