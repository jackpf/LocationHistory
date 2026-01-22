package com.jackpf.locationhistory.server.util

import io.circe.Decoder
import sttp.client4.circe.asJson
import sttp.client4.{ResponseAs, asString}
import sttp.model.ResponseMetadata

import java.io.IOException
import scala.util.{Failure, Success, Try}

object STTPUtils {
  private def toTry[E, R](response: Either[E, R], meta: ResponseMetadata): Try[R] = {
    response match {
      case Right(body) =>
        Success(body)
      case Left(error) =>
        Failure(new IOException(s"Request to ${meta.statusText} failed: ${error}"))
    }
  }

  val asTryString: ResponseAs[Try[String]] = {
    asString.mapWithMetadata { (either, meta) => toTry(either, meta) }
  }

  val asTryUnit: ResponseAs[Try[Unit]] = {
    asString.mapWithMetadata { (either, meta) => toTry(either, meta).map(_ => ()) }
  }

  def asTryJson[T: Decoder]: ResponseAs[Try[T]] = {
    asJson[T].mapWithMetadata { (either, meta) => toTry(either, meta) }
  }
}
