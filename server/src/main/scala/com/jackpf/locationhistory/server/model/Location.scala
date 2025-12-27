package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.beacon_service.Location as ProtoLocation

object Location {
  def fromProto(
      proto: ProtoLocation,
      timestamp: Long = System.currentTimeMillis()
  ): Location = Location(
    lat = proto.lat,
    lon = proto.lon,
    accuracy = proto.accuracy,
    timestamp = timestamp
  )
}

case class Location(lat: Double, lon: Double, accuracy: Double, timestamp: Long)
