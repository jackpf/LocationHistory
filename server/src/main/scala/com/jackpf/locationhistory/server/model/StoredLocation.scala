package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.StoredLocation as ProtoStoredLocation

object StoredLocation {
  def fromLocation(location: Location, timestamp: Long): StoredLocation =
    StoredLocation(location, timestamp)
}

case class StoredLocation(location: Location, timestamp: Long) {
  def toProto: ProtoStoredLocation = ProtoStoredLocation(
    location = Some(location.toProto),
    timestamp = timestamp
  )
}
