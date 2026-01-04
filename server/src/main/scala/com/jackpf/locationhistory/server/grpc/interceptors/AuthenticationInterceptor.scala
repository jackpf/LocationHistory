package com.jackpf.locationhistory.server.grpc.interceptors

import com.jackpf.locationhistory.server.grpc.AuthenticationManager
import io.grpc.*

class AuthenticationInterceptor(
    authenticationManager: AuthenticationManager,
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

    if (
      ignoredMethodNames.contains(methodName) ||
      authenticationManager.isValidPassword(authHeader)
    ) {
      next.startCall(call, headers)
    } else {
      call.close(Status.UNAUTHENTICATED.withDescription("Invalid password"), headers)
      new ServerCall.Listener[ReqT] {}
    }
  }
}
