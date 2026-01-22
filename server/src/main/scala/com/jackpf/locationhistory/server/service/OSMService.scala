package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.server.service.OSMService.{GeoLookupResponse, cache}
import com.jackpf.locationhistory.server.util.ConcurrentInMemoryCache
import com.jackpf.locationhistory.server.util.STTPUtils.*
import io.circe.generic.auto.*
import sttp.client4.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object OSMService {

  /** Use a simple in-memory cache for OSM responses
    * Helps in cases where we're stationary for a long time (no need to continuously look up locations)
    */
  private val cache: ConcurrentInMemoryCache[(Long, Long), GeoLookupResponse] =
    new ConcurrentInMemoryCache(1024)

  private def geoLookupUrl(lat: Double, lon: Double): String =
    s"https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&extratags=1&format=jsonv2"

  object GeoLookupResponse {

    /** Address fields are all optional
      * Availability depends on OSM data for the location
      */
    case class Address(
        house_number: Option[String] = None,
        road: Option[String] = None,
        quarter: Option[String] = None,
        suburb: Option[String] = None,
        borough: Option[String] = None,
        city: Option[String] = None,
        town: Option[String] = None,
        village: Option[String] = None,
        county: Option[String] = None,
        state: Option[String] = None,
        state_district: Option[String] = None,
        `ISO3166-2-lvl4`: Option[String] = None,
        postcode: Option[String] = None,
        country: Option[String] = None,
        country_code: Option[String] = None
    )
  }
  case class GeoLookupResponse(
      place_id: Long,
      licence: String,
      osm_type: String,
      osm_id: Long,
      lat: String,
      lon: String,
      category: String,
      `type`: String,
      place_rank: Int,
      importance: Double,
      addresstype: String,
      /** Can be null for unnamed places */
      name: Option[String],
      display_name: String,
      address: GeoLookupResponse.Address,
      extratags: Option[Map[String, String]],
      boundingbox: Seq[String]
  )
}

class OSMService(backend: Backend[Future]) {

  /** Drop precision for cache keys to 4 points (~11m accuracy)
    * Otherwise we'll never get cache hits
    */
  private def toCacheKey(c: Double): Long = {
    Math.round(c * 10000)
  }

  def reverseGeoLookup(lat: Double, lon: Double)(using
      ec: ExecutionContext
  ): Future[Try[GeoLookupResponse]] = {
    cache.getOrElseTry(
      (toCacheKey(lat), toCacheKey(lon)),
      basicRequest
        .header("Content-Type", "application/json")
        .header("User-Agent", "LocationHistory/1.0 (https://github.com/jackpfarrelly/location-history)")
        .get(uri"${OSMService.geoLookupUrl(lat, lon)}")
        .response(asTryJson[GeoLookupResponse])
        .send(backend)
        .map(_.body)
    )
  }
}
