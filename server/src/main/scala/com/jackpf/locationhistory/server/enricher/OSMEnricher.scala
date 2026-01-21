package com.jackpf.locationhistory.server.enricher

import com.jackpf.locationhistory.server.model.Location
import com.jackpf.locationhistory.server.service.OSMService
import com.jackpf.locationhistory.server.util.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class OSMEnricher(osmService: OSMService) extends MetadataEnricher with Logging {
  private def extraTagsToMap(extraTags: Map[String, String]): Map[String, String] =
    extraTags.map { case (key, value) =>
      s"tag:${key}" -> value
    }

  private def metaToMap(meta: OSMService.GeoLookupResponse): Map[String, String] =
    Map(
      "name" -> meta.name,
      "category" -> Some(meta.category),
      "type" -> Some(meta.`type`),
      "houseNumber" -> meta.address.houseNumber,
      "road" -> meta.address.road,
      "quarter" -> meta.address.quarter,
      "suburb" -> meta.address.suburb,
      "borough" -> meta.address.borough,
      "city" -> meta.address.city,
      "town" -> meta.address.town,
      "village" -> meta.address.village,
      "county" -> meta.address.county,
      "state" -> meta.address.state,
      "postcode" -> meta.address.postcode,
      "country" -> meta.address.country,
      "countryCode" -> meta.address.countryCode
    ).collect { case (k, Some(v)) => k -> v } ++ extraTagsToMap(meta.extratags.getOrElse(Map.empty))

  override def enrich(
      location: Location
  )(using ec: ExecutionContext): Future[Map[String, String]] = {
    osmService.reverseGeoLookup(location.lat, location.lon).map {
      case Failure(exception) =>
        log.error("Error fetching", exception)
        Map.empty
      case Success(meta) =>
        metaToMap(meta)
    }
  }
}
