package com.jackpf.locationhistory.server.grpc.interceptors

import io.grpc.*

import scala.util.Failure

class AuthenticationInterceptor(
    tokenService: TokenService,
    ignoredMethodNames: Set[String]
) extends ServerInterceptor {
  private val AuthKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    val authHeader = Option(headers.get(AuthKey)).getOrElse("").replaceFirst("Bearer\\s+", "")
    val methodName = call.getMethodDescriptor.getFullMethodName

    tokenService.decodeToken(authHeader) match {
      case Failure(exception) if !ignoredMethodNames.contains(methodName) =>
        call.close(
          Status.UNAUTHENTICATED.withDescription(
            s"Authentication failure: ${exception.getMessage}"
          ),
          headers
        )
        new ServerCall.Listener[ReqT] {}
      case _ => next.startCall(call, headers)
    }
  }
}
