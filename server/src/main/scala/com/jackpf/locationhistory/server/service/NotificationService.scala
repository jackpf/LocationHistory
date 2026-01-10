package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.admin_service.NotificationType
import com.jackpf.locationhistory.server.service.NotificationService.Notification

import scala.concurrent.{ExecutionContext, Future}
import sttp.client4.*
import sttp.model.Uri

import java.io.IOException
import scala.util.{Failure, Success, Try}
import com.jackpf.locationhistory.server.util.ParamExtractor.*

object NotificationService {
  object Notification {
    def fromProto(proto: NotificationType): Option[Notification] = proto match {
      case NotificationType.REQUEST_BEACON  => Some(Notification.TRIGGER_BEACON)
      case NotificationType.REQUEST_ALARM   => Some(Notification.TRIGGER_ALARM)
      case NotificationType.Unrecognized(_) => None
    }
  }

  enum Notification {
    case TRIGGER_BEACON, TRIGGER_ALARM

    def message: String = toString
  }
}

class NotificationService(backend: Backend[Future]) {
  def sendNotification(url: String, notification: Notification)(using
      ec: ExecutionContext
  ): Future[Try[Unit]] = {
    for {
      uri <- Try(Uri(url)).toFuture
      request = quickRequest
        .header("content-type", "text/plain")
        .body(notification.message)
        .post(Uri(url))
      response <- backend.send(request).map { response =>
        if (response.isSuccess) Success(())
        else Failure(new IOException(s"Request failed: ${response.body}"))
      }
    } yield response
  }
}
