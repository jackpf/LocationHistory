package com.jackpf.locationhistory.server.grpc.util

import com.jackpf.locationhistory.server.errors.ApplicationError
import com.jackpf.locationhistory.server.grpc.util.ErrorMapper.*
import io.grpc.*

object ErrorMapper {
  private def applicationErrorToStatus(
      applicationError: ApplicationError
  ): Status = applicationError.status
    .withDescription(applicationError.getMessage)
    .withCause(applicationError)

  extension (throwable: Throwable) {
    def toGrpcStatus: Status = {
      throwable match {
        case applicationError: ApplicationError =>
          println(
            s"LALA Mapping application error -> ${applicationError.status}: ${applicationError.getMessage}"
          )
          applicationErrorToStatus(applicationError)
        case other =>
          println(
            s"LALA Mapping OTHER error: ${other.getMessage}"
          )
          Status.INTERNAL.withDescription(other.getMessage).withCause(other)
      }
    }
  }
}

class ExceptionInterceptor extends ServerInterceptor {
  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    println("LALA Intercept")
    val delegate = next.startCall(call, headers)
    println("LALA Intercepted")

    new ServerCall.Listener[ReqT]() {
      private def safeRun(block: => Unit): Unit =
        try block
        catch {
          case t: Throwable =>
            println("LALA Caught exception")
            val status = t.toGrpcStatus
            throw status.asRuntimeException() // safe for unary calls
        }

      override def onMessage(msg: ReqT): Unit = safeRun {
        delegate.onMessage(msg)
      }

      override def onHalfClose(): Unit = safeRun {
        delegate.onHalfClose()
      }

      override def onCancel(): Unit = safeRun {
        delegate.onCancel()
      }

      override def onComplete(): Unit = safeRun {
        delegate.onComplete()
      }

      override def onReady(): Unit = safeRun {
        delegate.onReady()
      }
    }
  }
}
