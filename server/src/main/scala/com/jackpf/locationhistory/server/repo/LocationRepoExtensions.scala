package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}
import com.jackpf.locationhistory.server.repo.LocationRepoExtensions.CheckDuplicateLocationFunc

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object LocationRepoExtensions {
  type CheckDuplicateLocationFunc = (Location, Long, StoredLocation) => Boolean
}

trait LocationRepoExtensions { self: LocationRepo =>

  /** Note that this is a "best effort" approach and not strictly thread safe:
    * race conditions can occur between checking previous location and update/insert logic
    * But for the sake of code readability and adhering to our functional interface,
    * we make this tradeoff
    * Location updates should not be so high frequency that this becomes a real problem,
    * and duplicate location points is not a breaking issue if it happens
    */
  def storeDeviceLocationOrUpdatePrevious(
      deviceId: DeviceId.Type,
      location: Location,
      timestamp: Long,
      isDuplicate: CheckDuplicateLocationFunc
  )(using ec: ExecutionContext): Future[Try[Unit]] = {
    getForDevice(deviceId, limit = Some(1)).map(_.headOption).flatMap {
      case Some(previousLocation) if isDuplicate(location, timestamp, previousLocation) =>
        update(
          deviceId,
          id = previousLocation.id,
          // TODO We might want to update an endTimestamp and count so we don't lose info of when the location was first seen
          updateAction = _.copy(timestamp = timestamp)
        )
      case _ =>
        storeDeviceLocation(deviceId, location, timestamp)
    }
  }
}
