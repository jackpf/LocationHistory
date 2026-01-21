package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.server.service.OSMService.GeoLookupResponse
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}
import io.circe.parser.decode

class OSMServiceTest extends DefaultSpecification {
  trait Context extends DefaultScope {
    val sampleJson: String =
      """{
        |  "place_id": 123456789,
        |  "licence": "Data © OpenStreetMap contributors",
        |  "osm_type": "way",
        |  "osm_id": 987654321,
        |  "lat": "51.5074",
        |  "lon": "-0.1278",
        |  "category": "building",
        |  "type": "yes",
        |  "place_rank": 30,
        |  "importance": 0.12345,
        |  "addresstype": "building",
        |  "name": "Test Building",
        |  "display_name": "Test Building, Test Road, London, UK",
        |  "address": {
        |    "house_number": "123",
        |    "road": "Test Road",
        |    "quarter": "Test Quarter",
        |    "suburb": "Test Suburb",
        |    "borough": "Test Borough",
        |    "city": "London",
        |    "ISO3166-2-lvl4": "GB-ENG",
        |    "postcode": "SW1A 1AA",
        |    "country": "United Kingdom",
        |    "country_code": "gb"
        |  },
        |  "extratags": {
        |    "building": "yes"
        |  },
        |  "boundingbox": ["51.5073", "51.5075", "-0.1279", "-0.1277"]
        |}""".stripMargin

    def decodeResponse: Either[io.circe.Error, GeoLookupResponse] =
      decode[GeoLookupResponse](sampleJson)
  }

  "OSMService GeoLookupResponse decoder" should {
    "decode snake_case JSON from Nominatim API" >> in(new Context {}) { context =>
      context.decodeResponse must beRight
    }

    "correctly parse place_id as placeId" >> in(new Context {}) { context =>
      context.decodeResponse.map(_.placeId) must beRight(123456789L)
    }

    "correctly parse osm_type as osmType" >> in(new Context {}) { context =>
      context.decodeResponse.map(_.osmType) must beRight("way")
    }

    "correctly parse osm_id as osmId" >> in(new Context {}) { context =>
      context.decodeResponse.map(_.osmId) must beRight(987654321L)
    }

    "correctly parse place_rank as placeRank" >> in(new Context {}) { context =>
      context.decodeResponse.map(_.placeRank) must beRight(30)
    }

    "correctly parse display_name as displayName" >> in(new Context {}) { context =>
      context.decodeResponse.map(_.displayName) must beRight("Test Building, Test Road, London, UK")
    }

    "correctly parse nested address with snake_case fields" >> in(new Context {}) { context =>
      val result = context.decodeResponse
      result must beRight
      result.map(_.address.houseNumber) must beRight(Some("123"))
      result.map(_.address.countryCode) must beRight(Some("gb"))
    }

    "correctly parse extratags" >> in(new Context {}) { context =>
      context.decodeResponse.map(_.extratags) must beRight(Some(Map("building" -> "yes")))
    }

    "correctly parse name as Option" >> in(new Context {}) { context =>
      context.decodeResponse.map(_.name) must beRight(Some("Test Building"))
    }

    "handle null name" >> in(new Context {
      override val sampleJson: String =
        """{
          |  "place_id": 123456789,
          |  "licence": "Data",
          |  "osm_type": "way",
          |  "osm_id": 987654321,
          |  "lat": "51.5074",
          |  "lon": "-0.1278",
          |  "category": "highway",
          |  "type": "residential",
          |  "place_rank": 26,
          |  "importance": 0.1,
          |  "addresstype": "road",
          |  "name": null,
          |  "display_name": "Test Road, London, UK",
          |  "address": {
          |    "road": "Test Road",
          |    "city": "London",
          |    "country": "United Kingdom",
          |    "country_code": "gb"
          |  },
          |  "boundingbox": ["51.5073", "51.5075", "-0.1279", "-0.1277"]
          |}""".stripMargin
    }) { context =>
      val result = context.decodeResponse
      result must beRight
      result.map(_.name) must beRight(None)
    }

    "handle missing extratags" >> in(new Context {
      override val sampleJson: String =
        """{
          |  "place_id": 123456789,
          |  "licence": "Data",
          |  "osm_type": "way",
          |  "osm_id": 987654321,
          |  "lat": "51.5074",
          |  "lon": "-0.1278",
          |  "category": "highway",
          |  "type": "residential",
          |  "place_rank": 26,
          |  "importance": 0.1,
          |  "addresstype": "road",
          |  "name": "Test Road",
          |  "display_name": "Test Road, London, UK",
          |  "address": {
          |    "road": "Test Road",
          |    "city": "London",
          |    "country": "United Kingdom",
          |    "country_code": "gb"
          |  },
          |  "boundingbox": ["51.5073", "51.5075", "-0.1279", "-0.1277"]
          |}""".stripMargin
    }) { context =>
      val result = context.decodeResponse
      result must beRight
      result.map(_.extratags) must beRight(None)
    }

    "handle minimal address fields" >> in(new Context {
      override val sampleJson: String =
        """{
          |  "place_id": 123456789,
          |  "licence": "Data",
          |  "osm_type": "node",
          |  "osm_id": 987654321,
          |  "lat": "51.5074",
          |  "lon": "-0.1278",
          |  "category": "place",
          |  "type": "village",
          |  "place_rank": 16,
          |  "importance": 0.5,
          |  "addresstype": "village",
          |  "name": "Small Village",
          |  "display_name": "Small Village, Some Country",
          |  "address": {
          |    "village": "Small Village",
          |    "country": "Some Country",
          |    "country_code": "xx"
          |  },
          |  "boundingbox": ["51.5", "51.6", "-0.2", "-0.1"]
          |}""".stripMargin
    }) { context =>
      val result = context.decodeResponse
      result must beRight
      result.map(_.address.village) must beRight(Some("Small Village"))
      result.map(_.address.city) must beRight(None)
      result.map(_.address.road) must beRight(None)
    }
  }
}
