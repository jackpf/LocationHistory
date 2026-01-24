package com.jackpf.locationhistory.server.enricher

object ConfiguredEnrichers {
  def fromConfigured(
      names: Seq[String],
      enrichers: Seq[MetadataEnricher]
  ): Seq[MetadataEnricher] = {
    names.map { name =>
      enrichers
        .find(_.name == name)
        .fold(
          throw new IllegalArgumentException(
            s"Invalid enricher: ${name}, available: ${enrichers.map(_.name).mkString(", ")}"
          )
        )(identity)
    }
  }
}
