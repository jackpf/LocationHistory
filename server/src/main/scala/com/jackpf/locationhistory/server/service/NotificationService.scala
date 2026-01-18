package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.admin_service.NotificationType
import com.jackpf.locationhistory.notifications.Notification.Message.{TriggerAlarm, TriggerLocation}
import com.jackpf.locationhistory.notifications.{
  LocationAccuracyRequest,
  AlarmNotification,
  LocationNotification,
  Notification as NotificationMessage
}
import com.jackpf.locationhistory.server.service.NotificationService.Notification
import sttp.client4.*

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

    def toMessage: NotificationMessage = this match {
      case Notification.TRIGGER_BEACON =>
        // TODO Allow high accuracy
        NotificationMessage(
          TriggerLocation(
            LocationNotification(requestAccuracy = LocationAccuracyRequest.BALANCED)
          )
        )
      case Notification.TRIGGER_ALARM =>
        NotificationMessage(
          TriggerAlarm(
            AlarmNotification()
          )
        )
    }
  }
}

class NotificationService(backend: Backend[Future]) {
  def sendNotification(url: String, notification: Notification)(using
      ec: ExecutionContext
  ): Future[Try[Unit]] = {
    val request = quickRequest
      .header("content-type", "text/plain")
      .body(notification.toMessage.toByteArray)
      .post(uri"${url}")

    for {
      response <- backend.send(request).map { response =>
        if (response.isSuccess) Success(())
        else Failure(new IOException(s"Request failed: ${response.body}"))
      }
    } yield response
  }
}
