package com.jackpf.locationhistory.server.grpc.interceptors

import com.jackpf.locationhistory.server.testutil.{
  DefaultScope,
  DefaultSpecification,
  MockServerCall
}
import io.grpc.ServerCall
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify, when}

import java.time.Instant
import scala.util.{Failure, Success}

class AuthenticationInterceptorTest extends DefaultSpecification {
  trait Context extends DefaultScope {
    val tokenService: TokenService = mock(classOf[TokenService])
    lazy val ignoredMethodNames: Set[String] = Set.empty
    lazy val interceptor: AuthenticationInterceptor =
      new AuthenticationInterceptor(tokenService, ignoredMethodNames)

    val mockCall: MockServerCall[String, String] = new MockServerCall[String, String]()

    val validToken: TokenService.Token = TokenService.Token(
      content = TokenService.Content(user = "test-user"),
      issuedAt = Instant.now(),
      expiresAt = Instant.now().plusSeconds(3600)
    )

    lazy val result: ServerCall.Listener[String] = mockCall.intercept(interceptor)
  }

  trait ValidTokenContext extends Context {
    when(tokenService.decodeToken(any[String]())).thenReturn(Success(validToken))
  }

  trait InvalidTokenContext extends Context {
    val tokenError: Exception = new IllegalArgumentException("Invalid token")
    when(tokenService.decodeToken(any[String]())).thenReturn(Failure(tokenError))
  }

  "AuthenticationInterceptor" should {
    "allow request with valid token" >> in(new ValidTokenContext {
      mockCall.putHeader("authorization", "Bearer valid-token")
    }) { context =>
      context.mockCall.verifyAllowed(context.result)
    }

    "allow request with valid token without Bearer prefix" >> in(new ValidTokenContext {
      mockCall.putHeader("authorization", "valid-token")
    }) { context =>
      context.mockCall.verifyAllowed(context.result)
    }

    "reject request with invalid token" >> in(new InvalidTokenContext {
      mockCall.putHeader("authorization", "Bearer invalid-token")
    }) { context =>
      context.mockCall.verifyRejectedUnauthenticated(context.result)
    }

    "reject request with missing auth header" >> in(new InvalidTokenContext {}) { context =>
      context.mockCall.verifyRejectedUnauthenticated(context.result)
    }

    "allow request with invalid token for ignored method" >> in(new InvalidTokenContext {
      override lazy val ignoredMethodNames: Set[String] = Set("test.Service/TestMethod")
    }) { context =>
      context.mockCall.verifyAllowed(context.result)
    }

    "reject request with invalid token for non-ignored method" >> in(new InvalidTokenContext {
      override lazy val ignoredMethodNames: Set[String] = Set("test.Service/OtherMethod")
      mockCall.putHeader("authorization", "Bearer invalid-token")
    }) { context =>
      context.mockCall.verifyRejectedUnauthenticated(context.result)
    }

    "strip Bearer prefix from token before decoding" >> in(new ValidTokenContext {
      mockCall.putHeader("authorization", "Bearer my-special-token")
    }) { context =>
      context.result
      verify(context.tokenService).decodeToken("my-special-token")
      ok
    }
  }
}
