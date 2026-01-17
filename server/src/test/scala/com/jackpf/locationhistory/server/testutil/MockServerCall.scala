package com.jackpf.locationhistory.server.testutil

import io.grpc.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, verify, when}
import org.specs2.execute.Result
import org.specs2.execute.ResultImplicits.combineResult
import org.specs2.matcher.Expectations.===
import org.specs2.matcher.MustMatchers.must
import org.specs2.matcher.Matchers

class MockServerCall[ReqT, RespT](
    val methodName: String = "test.Service/TestMethod"
) extends Matchers {
  val call: ServerCall[ReqT, RespT] = mock(classOf[ServerCall[ReqT, RespT]])
  val headers: Metadata = new Metadata()
  val next: ServerCallHandler[ReqT, RespT] = mock(classOf[ServerCallHandler[ReqT, RespT]])
  val listener: ServerCall.Listener[ReqT] = mock(classOf[ServerCall.Listener[ReqT]])

  private val methodDescriptor: MethodDescriptor[ReqT, RespT] =
    mock(classOf[MethodDescriptor[ReqT, RespT]])

  when(call.getMethodDescriptor).thenReturn(methodDescriptor)
  when(methodDescriptor.getFullMethodName).thenReturn(methodName)
  when(next.startCall(any[ServerCall[ReqT, RespT]](), any[Metadata]())).thenReturn(listener)

  def putHeader(key: String, value: String): Unit = {
    val metadataKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
    headers.put(metadataKey, value)
  }

  def intercept(interceptor: ServerInterceptor): ServerCall.Listener[ReqT] =
    interceptor.interceptCall(call, headers, next)

  def verifyAllowed(result: ServerCall.Listener[ReqT]): Result = {
    verify(next).startCall(call, headers)
    verify(call, never()).close(any[Status](), any[Metadata]())
    result === listener
  }

  def verifyRejected(result: ServerCall.Listener[ReqT], expectedCode: Status.Code): Result = {
    val statusCaptor: ArgumentCaptor[Status] = ArgumentCaptor.forClass(classOf[Status])
    verify(call).close(statusCaptor.capture(), any[Metadata]())
    verify(next, never()).startCall(any[ServerCall[ReqT, RespT]](), any[Metadata]())
    (result must not(beEqualTo(listener))) and
      (statusCaptor.getValue.getCode === expectedCode)
  }

  def verifyRejectedUnauthenticated(result: ServerCall.Listener[ReqT]): Result =
    verifyRejected(result, Status.UNAUTHENTICATED.getCode)
}
