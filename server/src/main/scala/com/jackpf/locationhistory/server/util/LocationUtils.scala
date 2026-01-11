package com.jackpf.locationhistory.server.util

import scala.math.*
import com.jackpf.locationhistory.server.model.Location

object LocationUtils {
  private val EarthRadiusMeters = 6371000.0

  def distanceMeters(lat1: Double, lon1: Double)(lat2: Double, lon2: Double): Double = {
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)

    val a = pow(sin(dLat / 2), 2) +
      cos(toRadians(lat1)) * cos(toRadians(lat2)) *
      pow(sin(dLon / 2), 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    EarthRadiusMeters * c
  }

  def isDuplicate(location1: Location, location2: Location, thresholdMeters: Int): Boolean = {
    distanceMeters(location1.lat, location1.lon)(location2.lat, location2.lon) < thresholdMeters
  }
}
