package com.jackpf.locationhistory.server.enricher

import com.jackpf.locationhistory.server.model.Location
import com.jackpf.locationhistory.server.service.OSMService
import com.jackpf.locationhistory.server.util.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class OSMEnricher(osmService: OSMService) extends MetadataEnricher with Logging {
  override def enrich(
      location: Location
  )(using ec: ExecutionContext): Future[Map[String, String]] = {
    osmService.reverseGeoLookup(location.lat, location.lon).map {
      case Failure(exception) =>
        log.error("Error fetching", exception)
        Map.empty // TODO Do we want to swallow errors here?
      case Success(meta) =>
        Map(
          "name" -> meta.name,
          "category" -> meta.category,
          "type" -> meta.`type`,
          "houseNumber" -> meta.address.houseNumber,
          "road" -> meta.address.road,
          "quarter" -> meta.address.quarter,
          "suburb" -> meta.address.suburb,
          "borough" -> meta.address.borough,
          "city" -> meta.address.city,
          "postcode" -> meta.address.postcode,
          "country" -> meta.address.country,
          "countryCode" -> meta.address.countryCode
        )
    }
  }
}
