package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.server.util.STTPUtils.*
import io.circe.derivation.{Configuration, ConfiguredDecoder}
import sttp.client4.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object OSMService {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  private def geoLookupUrl(lat: Double, lon: Double): String =
    s"https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&extratags=1&format=jsonv2"

  object GeoLookupResponse {

    /** Address fields are all optional
      * Availability depends on OSM data for the location
      */
    case class Address(
        houseNumber: Option[String] = None,
        road: Option[String] = None,
        quarter: Option[String] = None,
        suburb: Option[String] = None,
        borough: Option[String] = None,
        city: Option[String] = None,
        town: Option[String] = None,
        village: Option[String] = None,
        county: Option[String] = None,
        state: Option[String] = None,
        stateDistrict: Option[String] = None,
        ISO31662Lvl4: Option[String] = None,
        postcode: Option[String] = None,
        country: Option[String] = None,
        countryCode: Option[String] = None
    ) derives ConfiguredDecoder
  }
  case class GeoLookupResponse(
      placeId: Long,
      licence: String,
      osmType: String,
      osmId: Long,
      lat: String,
      lon: String,
      category: String,
      `type`: String,
      placeRank: Int,
      importance: Double,
      addresstype: String,
      /** Can be null for unnamed places */
      name: Option[String],
      displayName: String,
      address: GeoLookupResponse.Address,
      extratags: Option[Map[String, String]],
      boundingbox: Seq[String]
  ) derives ConfiguredDecoder
}

class OSMService(backend: Backend[Future]) {
  import OSMService.*

  def reverseGeoLookup(lat: Double, lon: Double)(using
      ec: ExecutionContext
  ): Future[Try[GeoLookupResponse]] = {
    basicRequest
      .header("content-type", "application/json")
      .get(uri"${OSMService.geoLookupUrl(lat, lon)}")
      .response(asTryJson[GeoLookupResponse])
      .send(backend)
      .map(_.body)
  }
}
