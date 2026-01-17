package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.server.grpc.interceptors.TokenService
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}

import java.time.Instant
import scala.util.{Success, Try}

class JwtAuthServiceTest extends DefaultSpecification {
  trait Context extends DefaultScope {
    val jwtAuthService: JwtAuthService = new JwtAuthService
    val testUser: String = "test-user"
    val testContent: TokenService.Content = TokenService.Content(user = testUser)
    val expireInSeconds: Long = 3600
  }

  trait EncodedTokenContext extends Context {
    val beforeEncode: Instant = Instant.now()
    lazy val token: String = jwtAuthService.encodeToken(testContent, expireInSeconds)
    lazy val afterEncode: Instant = { token; Instant.now() }
  }

  trait DecodedTokenContext extends EncodedTokenContext {
    lazy val decoded: Try[TokenService.Token] = jwtAuthService.decodeToken(token)
  }

  "JwtAuthService" should {
    "encode a token" >> in(new EncodedTokenContext {}) { context =>
      context.token must not(beEmpty)
    }

    "decode a valid token" >> in(new DecodedTokenContext {}) { context =>
      context.decoded must beSuccessfulTry
    }

    "decode token with correct user content" >> in(new DecodedTokenContext {}) { context =>
      context.decoded.map(_.content.user) === Success(context.testUser)
    }

    "decode token with valid issuedAt timestamp" >> in(new DecodedTokenContext {}) { context =>
      context.decoded must beSuccessfulTry
      context.decoded.get.issuedAt.getEpochSecond must beBetween(
        context.beforeEncode.getEpochSecond,
        context.afterEncode.getEpochSecond
      )
    }

    "decode token with valid expiresAt timestamp" >> in(new DecodedTokenContext {}) { context =>
      context.decoded must beSuccessfulTry
      context.decoded.get.expiresAt.getEpochSecond must beBetween(
        context.beforeEncode.plusSeconds(context.expireInSeconds).getEpochSecond,
        context.afterEncode.plusSeconds(context.expireInSeconds).getEpochSecond
      )
    }

    "fail to decode an invalid token" >> in(new Context {}) { context =>
      val decoded = context.jwtAuthService.decodeToken("invalid.token.string")
      decoded must beFailedTry
    }

    "fail to decode a tampered token" >> in(new EncodedTokenContext {}) { context =>
      val tamperedToken = context.token.dropRight(5) + "xxxxx"
      val decoded = context.jwtAuthService.decodeToken(tamperedToken)
      decoded must beFailedTry
    }

    "encode and decode round trip preserves content" >> in(new Context {}) { context =>
      val content = TokenService.Content(user = "another-user")
      val token = context.jwtAuthService.encodeToken(content, context.expireInSeconds)
      val decoded = context.jwtAuthService.decodeToken(token)

      decoded must beSuccessfulTry
      decoded.get.content === content
    }
  }
}
