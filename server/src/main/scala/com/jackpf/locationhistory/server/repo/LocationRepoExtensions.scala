package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object LocationRepoExtensions {
  extension (repo: LocationRepo) {

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
        isDuplicateFunc: ((Location, Option[Location])) => Boolean
    )(using ec: ExecutionContext): Future[Try[Unit]] = {
      for {
        previousLocation <- repo.getForDevice(deviceId, limit = Some(1)).map(_.headOption)
        isDuplicate = isDuplicateFunc(location, previousLocation.map(_.location))
        insertOrUpdate <-
          if (!isDuplicate) repo.storeDeviceLocation(deviceId, location, timestamp)
          else
            repo.update(
              deviceId,
              id = previousLocation.get.id,
              updateAction = storedLocation => storedLocation.copy(timestamp = timestamp)
            )
      } yield insertOrUpdate
    }
  }
}
