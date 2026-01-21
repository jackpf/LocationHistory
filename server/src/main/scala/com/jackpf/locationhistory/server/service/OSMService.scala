package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.server.util.STTPUtils.*
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import sttp.client4.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object OSMService {
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  private def geoLookupUrl(lat: Double, lon: Double): String =
    s"https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&extratags=1&format=jsonv2"

  object GeoLookupResponse {
    case class Address(
        houseNumber: String,
        road: String,
        quarter: String,
        suburb: String,
        borough: String,
        city: String,
        ISO31662Lvl4: String,
        postcode: String,
        country: String,
        countryCode: String
    )
  }
  case class GeoLookupResponse(
      placeId: Long,
      license: String,
      osmType: String,
      osmId: Long,
      lat: String,
      lon: String,
      category: String,
      `type`: String,
      placeRank: Int,
      importance: Double,
      addressType: String,
      name: String,
      displayName: String,
      address: GeoLookupResponse.Address,
      extraTags: Map[String, String],
      boundingbox: Seq[String]
  )
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
