package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.notifications.Notification
import com.jackpf.locationhistory.server.util.STTPUtils.*
import sttp.client4.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NotificationService(backend: Backend[Future]) {
  def sendNotification(url: String, notification: Notification)(using
      ec: ExecutionContext
  ): Future[Try[Unit]] = {
    basicRequest
      .header("content-type", "application/octet-stream")
      .body(notification.toByteArray)
      .post(uri"${url}")
      .response(asTryUnit)
      .send(backend)
      .map(_.body)
  }
}
