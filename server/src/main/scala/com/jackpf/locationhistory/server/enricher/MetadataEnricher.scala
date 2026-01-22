package com.jackpf.locationhistory.server.enricher

import com.jackpf.locationhistory.server.model.Location

import scala.concurrent.{ExecutionContext, Future}

trait MetadataEnricher {
  val name: String

  def enrich(location: Location)(using ec: ExecutionContext): Future[Map[String, String]]
}
