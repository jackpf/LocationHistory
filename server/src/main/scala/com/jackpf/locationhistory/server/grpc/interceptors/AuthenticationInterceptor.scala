package com.jackpf.locationhistory.server.grpc.interceptors

import io.grpc.*

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class AuthenticationInterceptor(password: String) extends ServerInterceptor {
  private val BearerBytes = s"Bearer ${password}".getBytes(StandardCharsets.UTF_8)
  private val AuthKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    val authHeader = headers.get(AuthKey)

    if (
      authHeader != null && MessageDigest.isEqual(
        authHeader.getBytes(StandardCharsets.UTF_8),
        BearerBytes
      )
    ) {
      next.startCall(call, headers)
    } else {
      call.close(Status.UNAUTHENTICATED.withDescription("Invalid password"), headers)
      new ServerCall.Listener[ReqT] {}
    }
  }
}
