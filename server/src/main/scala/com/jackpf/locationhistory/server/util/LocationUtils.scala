package com.jackpf.locationhistory.server.util

import scala.math.*
import com.jackpf.locationhistory.server.model.{Location, StoredLocation}

object LocationUtils {
  private val EarthRadiusMeters = 6371000.0
  private val DuplicateThresholdMeters = 20

  def distanceMeters(lat1: Double, lon1: Double)(lat2: Double, lon2: Double): Double = {
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)

    val a = pow(sin(dLat / 2), 2) +
      cos(toRadians(lat1)) * cos(toRadians(lat2)) *
      pow(sin(dLon / 2), 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    EarthRadiusMeters * c
  }

  def isDuplicate(
      newLocation: Location,
      newTimestamp: Long,
      previousLocation: StoredLocation
  ): Boolean = {
    // Ignoring timestamps currently, but check it so we don't get compiler warnings...
    newTimestamp > 0 && distanceMeters(
      newLocation.lat,
      newLocation.lon
    )(
      previousLocation.location.lat,
      previousLocation.location.lon
    ) < DuplicateThresholdMeters
  }
}
