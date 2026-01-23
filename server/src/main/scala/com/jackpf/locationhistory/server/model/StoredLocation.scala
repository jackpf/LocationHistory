package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.StoredLocation as ProtoStoredLocation

object StoredLocation {
  def fromLocation(
      location: Location,
      id: Long,
      startTimestamp: Long,
      endTimestamp: Long,
      count: Long
  ): StoredLocation =
    StoredLocation(id, location, startTimestamp, endTimestamp, count)
}

case class StoredLocation(
    id: Long,
    location: Location,
    startTimestamp: Long,
    endTimestamp: Long,
    count: Long
) {
  def toProto: ProtoStoredLocation = ProtoStoredLocation(
    location = Some(location.toProto),
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    count = count
  )
}
