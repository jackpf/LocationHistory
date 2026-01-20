package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.notifications.Notification as NotificationMessage
import sttp.client4.*

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class NotificationService(backend: Backend[Future]) {
  def sendNotification(url: String, notification: NotificationMessage)(using
      ec: ExecutionContext
  ): Future[Try[Unit]] = {
    val request = quickRequest
      .header("content-type", "application/octet-stream")
      .body(notification.toByteArray)
      .post(uri"${url}")

    for {
      response <- backend.send(request).map { response =>
        if (response.isSuccess) Success(())
        else Failure(new IOException(s"Request failed: ${response.body}"))
      }
    } yield response
  }
}
