package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.StoredLocation as ProtoStoredLocation

object StoredLocation {
  object Metadata {
    def initial(timestamp: Long): Metadata =
      Metadata(startTimestamp = timestamp, endTimestamp = timestamp, count = 1L)
  }

  case class Metadata(
      startTimestamp: Long,
      endTimestamp: Long,
      count: Long
  ) {
    def updated(newTimestamp: Long): Metadata = {
      // Handle mismatched newTimestamp (e.g. out-of-order events)
      val newStartTimestamp = math.min(startTimestamp, newTimestamp)
      val newEndTimestamp = math.max(endTimestamp, newTimestamp)

      copy(startTimestamp = newStartTimestamp, endTimestamp = newEndTimestamp, count = count + 1)
    }
  }

  def fromLocation(
      location: Location,
      id: Long,
      metadata: Metadata
  ): StoredLocation =
    StoredLocation(id, location, metadata)
}

case class StoredLocation(
    id: Long,
    location: Location,
    metadata: StoredLocation.Metadata
) {
  def toProto: ProtoStoredLocation = ProtoStoredLocation(
    location = Some(location.toProto),
    startTimestamp = metadata.startTimestamp,
    endTimestamp = metadata.endTimestamp,
    count = metadata.count
  )
}
