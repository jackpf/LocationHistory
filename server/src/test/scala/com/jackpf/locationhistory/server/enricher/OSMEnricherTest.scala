package com.jackpf.locationhistory.server.enricher

import com.jackpf.locationhistory.server.model.Location
import com.jackpf.locationhistory.server.service.OSMService
import com.jackpf.locationhistory.server.service.OSMService.GeoLookupResponse
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, MockModels}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class OSMEnricherTest(implicit ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    val osmService: OSMService = mock(classOf[OSMService])
    val enricher: OSMEnricher = new OSMEnricher(osmService)
    val location: Location = MockModels.location(lat = 51.5074, lon = -0.1278)

    def geoLookupResponse: GeoLookupResponse = GeoLookupResponse(
      place_id = 123456789L,
      licence = "Data",
      osm_type = "way",
      osm_id = 987654321L,
      lat = "51.5074",
      lon = "-0.1278",
      category = "building",
      `type` = "yes",
      place_rank = 30,
      importance = 0.5,
      addresstype = "building",
      name = Some("Test Building"),
      display_name = "Test Building, Test Road, London, UK",
      address = GeoLookupResponse.Address(
        house_number = Some("123"),
        road = Some("Test Road"),
        city = Some("London"),
        postcode = Some("SW1A 1AA"),
        country = Some("United Kingdom"),
        country_code = Some("gb")
      ),
      extratags = Some(Map("building" -> "yes")),
      boundingbox = Seq("51.5073", "51.5075", "-0.1279", "-0.1277")
    )

    def serviceResponse: Future[Try[GeoLookupResponse]] =
      Future.successful(Success(geoLookupResponse))

    lazy val result: Future[Map[String, String]] = {
      when(osmService.reverseGeoLookup(any[Double](), any[Double]())(using any[ExecutionContext]()))
        .thenReturn(serviceResponse)
      enricher.enrich(location)
    }
  }

  "OSMEnricher" should {
    "extract metadata from successful geo lookup" >> in(new Context {}) { context =>
      context.result must contain(
        "name" -> "Test Building",
        "category" -> "building",
        "type" -> "yes",
        "houseNumber" -> "123",
        "road" -> "Test Road",
        "city" -> "London",
        "postcode" -> "SW1A 1AA",
        "country" -> "United Kingdom",
        "countryCode" -> "gb"
      ).await
    }

    "return empty map on failure" >> in(new Context {
      override def serviceResponse = Future.successful(Failure(new RuntimeException("API error")))
    }) { context =>
      context.result must beEqualTo(Map.empty[String, String]).await
    }

    "omit fields that are None" >> in(new Context {
      override def geoLookupResponse = GeoLookupResponse(
        place_id = 123456789L,
        licence = "Data",
        osm_type = "way",
        osm_id = 987654321L,
        lat = "51.5074",
        lon = "-0.1278",
        category = "building",
        `type` = "yes",
        place_rank = 30,
        importance = 0.5,
        addresstype = "building",
        name = None,
        display_name = "London, UK",
        address = GeoLookupResponse.Address(
          country = Some("United Kingdom"),
          country_code = Some("gb")
        ),
        extratags = None,
        boundingbox = Seq("51.5073", "51.5075", "-0.1279", "-0.1277")
      )
    }) { context =>
      context.result must not(haveKey("name")).await
      context.result must not(haveKey("city")).await
      context.result must haveKey("country").await
      context.result must haveKey("countryCode").await
    }

    "include extratags with tag: prefix" >> in(new Context {}) { context =>
      context.result must havePair("tag:building" -> "yes").await
    }
  }
}
