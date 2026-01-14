package com.jackpf.locationhistory.server.grpc.interceptors

import com.jackpf.locationhistory.server.grpc.interceptors.LoggingInterceptor.LogLevel
import com.jackpf.locationhistory.server.util.Logging
import io.grpc.*

object LoggingInterceptor {
  enum LogLevel {
    case DEBUG, INFO
  }
}

class LoggingInterceptor(level: LogLevel) extends ServerInterceptor with Logging {
  private def output(message: String): Unit = level match {
    case LogLevel.DEBUG => log.debug(message)
    case LogLevel.INFO  => log.info(message)
  }

  private def error(message: String, cause: Throwable): Unit = {
    log.error(message, cause)
  }

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    val methodName = call.getMethodDescriptor.getFullMethodName

    val loggingServerCall = new ForwardingServerCall.SimpleForwardingServerCall[ReqT, RespT](call) {
      override def delegate(): ServerCall[ReqT, RespT] = call

      override def close(status: Status, trailers: Metadata): Unit = {
        if (!status.isOk) {
          error(s"[gRPC] ${status.getCode}: ${status.getDescription}", status.getCause)
        }

        delegate().close(status, trailers)
      }
    }

    val listener = next.startCall(loggingServerCall, headers)

    new ForwardingServerCallListener[ReqT] {
      override def delegate(): ServerCall.Listener[ReqT] = listener

      override def onMessage(message: ReqT): Unit = {
        output(s"[gRPC] Requested $methodName: ${message}")
        delegate().onMessage(message)
      }

      override def onHalfClose(): Unit = {
        delegate().onHalfClose()
      }

      override def onCancel(): Unit = {
        output(s"[gRPC] Request canceled: $methodName")
        delegate().onCancel()
      }
    }
  }
}
