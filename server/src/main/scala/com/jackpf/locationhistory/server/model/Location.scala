package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.Location as ProtoLocation

object Location {
  def fromProto(
      proto: ProtoLocation
  ): Location = Location(
    lat = proto.lat,
    lon = proto.lon,
    accuracy = proto.accuracy,
    metadata = proto.metadata
  )
}

case class Location(lat: Double, lon: Double, accuracy: Double, metadata: Map[String, String]) {
  def toProto: ProtoLocation = ProtoLocation(
    lat = lat,
    lon = lon,
    accuracy = accuracy,
    metadata = metadata
  )
}
