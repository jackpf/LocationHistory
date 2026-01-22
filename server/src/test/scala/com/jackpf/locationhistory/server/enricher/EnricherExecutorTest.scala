package com.jackpf.locationhistory.server.enricher

import com.jackpf.locationhistory.server.model.Location
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, MockModels}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.{ExecutionContext, Future}

class EnricherExecutorTest(implicit ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    val location: Location = MockModels.location(metadata = Map.empty)

    def createMockEnricher(response: Future[Map[String, String]]): MetadataEnricher = {
      val enricher = mock(classOf[MetadataEnricher])
      when(enricher.enrich(any[Location]())(using any[ExecutionContext]()))
        .thenReturn(response)
      enricher
    }

    def successfulEnricher(metadata: Map[String, String]): MetadataEnricher =
      createMockEnricher(Future.successful(metadata))

    def failingEnricher(exception: Throwable): MetadataEnricher =
      createMockEnricher(Future.failed(exception))

    val enrichers: Seq[MetadataEnricher] = Seq(successfulEnricher(Map("key" -> "value")))
    lazy val executor: EnricherExecutor = new EnricherExecutor(enrichers)
    lazy val result: Future[Location] = executor.enrich(location)
  }

  "EnricherExecutor" should {
    "return location unchanged when no enrichers" >> in(new Context {
      override val enrichers: Seq[MetadataEnricher] = Seq.empty
    }) { context =>
      context.result.map(_.metadata) must beEqualTo(Map.empty[String, String]).await
    }

    "enrich location with single enricher" >> in(new Context {
      override val enrichers = Seq(successfulEnricher(Map("key1" -> "value1", "key2" -> "value2")))
    }) { context =>
      context.result.map(_.metadata) must beEqualTo(
        Map("key1" -> "value1", "key2" -> "value2")
      ).await
    }

    "merge metadata from multiple enrichers" >> in(new Context {
      override val enrichers = Seq(
        successfulEnricher(Map("name" -> "Test Place", "city" -> "London")),
        successfulEnricher(Map("weather" -> "sunny", "temp" -> "20C"))
      )
    }) { context =>
      context.result.map(_.metadata) must beEqualTo(
        Map(
          "name" -> "Test Place",
          "city" -> "London",
          "weather" -> "sunny",
          "temp" -> "20C"
        )
      ).await
    }

    "duplicate keys are overwritten" >> in(new Context {
      override val enrichers = Seq(
        successfulEnricher(Map("name" -> "First Name", "unique1" -> "value1")),
        successfulEnricher(Map("name" -> "Second Name", "unique2" -> "value2"))
      )
    }) { context =>
      context.result.map(_.metadata.get("name")) must beSome("Second Name").await
      context.result.map(_.metadata.get("unique1")) must beSome("value1").await
      context.result.map(_.metadata.get("unique2")) must beSome("value2").await
    }

    "recover from failing enricher and continue" >> in(new Context {
      override val enrichers = Seq(
        successfulEnricher(Map("from1" -> "value1")),
        failingEnricher(new RuntimeException("Enricher failed")),
        successfulEnricher(Map("from3" -> "value3"))
      )
    }) { context =>
      context.result.map(_.metadata) must beEqualTo(
        Map(
          "from1" -> "value1",
          "from3" -> "value3"
        )
      ).await
    }

    "return empty metadata when all enrichers fail" >> in(new Context {
      override val enrichers = Seq(
        failingEnricher(new RuntimeException("Error 1")),
        failingEnricher(new RuntimeException("Error 2"))
      )
    }) { context =>
      context.result.map(_.metadata) must beEqualTo(Map.empty[String, String]).await
    }

    "preserve original location coordinates" >> in(new Context {}) { context =>
      context.result.map(_.lat) must beEqualTo(context.location.lat).await
      context.result.map(_.lon) must beEqualTo(context.location.lon).await
      context.result.map(_.accuracy) must beEqualTo(context.location.accuracy).await
    }

    "append to existing metadata" >> in(new Context {
      override val location: Location = MockModels.location(metadata = Map("existing" -> "data"))
      override val enrichers = Seq(successfulEnricher(Map("new" -> "data")))
    }) { context =>
      context.result.map(_.metadata) must beEqualTo(
        Map(
          "existing" -> "data",
          "new" -> "data"
        )
      ).await
    }

    "handle enricher returning empty map" >> in(new Context {
      override val enrichers = Seq(
        successfulEnricher(Map("key" -> "value")),
        successfulEnricher(Map.empty)
      )
    }) { context =>
      context.result.map(_.metadata) must beEqualTo(Map("key" -> "value")).await
    }

    "handle large number of enrichers" >> in(new Context {
      override val enrichers = (1 to 10).map(i => successfulEnricher(Map(s"key$i" -> s"value$i")))
    }) { context =>
      context.result.map(_.metadata.size) must beEqualTo(10).await
      context.result.map(_.metadata.get("key1")) must beSome("value1").await
      context.result.map(_.metadata.get("key10")) must beSome("value10").await
    }
  }
}
