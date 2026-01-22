package com.jackpf.locationhistory.server.enricher

import com.jackpf.locationhistory.server.model.Location
import com.jackpf.locationhistory.server.util.Logging

import scala.concurrent.{ExecutionContext, Future}

class EnricherExecutor(enrichers: Iterable[MetadataEnricher]) extends Logging {
  private def recoverErrors(
      enricher: MetadataEnricher,
      result: Future[Map[String, String]]
  )(using ec: ExecutionContext): Future[Map[String, String]] = {
    result.recover { case e: Exception =>
      log.error(s"Enricher failed: ${enricher.name}", e)
      Map.empty
    }
  }

  /** Overwrites keys on duplicates
    */
  private def mergeMetadata(multiple: Iterable[Map[String, String]]): Map[String, String] =
    multiple.flatten.toMap

  def enrich(location: Location)(using
      ec: ExecutionContext
  ): Future[Location] = {
    val responses = enrichers.map(enricher => recoverErrors(enricher, enricher.enrich(location)))

    Future.sequence(responses).map { results =>
      val metadata = mergeMetadata(results)
      location.patchMetadata(metadata)
    }
  }
}
