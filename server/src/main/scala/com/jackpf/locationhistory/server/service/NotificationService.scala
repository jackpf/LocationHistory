package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.admin_service.NotificationType
import com.jackpf.locationhistory.server.service.NotificationService.Notification

import scala.concurrent.{ExecutionContext, Future}
import sttp.client4.*
import sttp.model.Uri

import java.io.IOException
import scala.util.{Failure, Success, Try}

object NotificationService {
  object Notification {
    def fromProto(proto: NotificationType): Option[Notification] =
      values.find(_.toString == proto.name)
  }

  enum Notification {
    case TRIGGER_BEACON, TRIGGER_ALARM

    def message: String = this match {
      case Notification.TRIGGER_BEACON => "TRIGGER_BEACON"
      case Notification.TRIGGER_ALARM  => "TRIGGER_ALARM"
    }
  }
}

class NotificationService(backend: Backend[Future]) {
  def sendNotification(url: String, notification: Notification)(using
      ec: ExecutionContext
  ): Future[Try[Unit]] = {
    val request = quickRequest
      .header("content-type", "text/plain")
      .body(notification.message)
      .post(Uri(url))

    backend.send(request).map { response =>
      if (response.isSuccess) Success(())
      else Failure(new IOException(s"Request failed: ${response.body}"))
    }
  }
}
