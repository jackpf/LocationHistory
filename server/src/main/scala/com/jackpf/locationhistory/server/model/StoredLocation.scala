package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.StoredLocation as ProtoStoredLocation

object StoredLocation {
  def fromLocation(location: Location, id: Long, timestamp: Long): StoredLocation =
    StoredLocation(id, location, timestamp)
}

case class StoredLocation(id: Long, location: Location, timestamp: Long) {
  def toProto: ProtoStoredLocation = ProtoStoredLocation(
    location = Some(location.toProto),
    timestamp = timestamp
  )
}
