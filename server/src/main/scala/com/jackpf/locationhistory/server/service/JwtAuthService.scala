package com.jackpf.locationhistory.server.service

import com.jackpf.locationhistory.server.grpc.interceptors.TokenService
import com.jackpf.locationhistory.server.service.JwtAuthService.{Algorithm, JwtData, Key}
import com.jackpf.locationhistory.server.util.ParamExtractor.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import scala.util.Try

object JwtAuthService {

  /** 256-bit random key
    * Note that this is stored locally in memory and is not shared across instances
    * If distributed authentication is needed, something file/database based is required
    */
  private val Key: String = {
    val bytes = new Array[Byte](32)
    new SecureRandom().nextBytes(bytes)
    Base64.getEncoder.encodeToString(bytes)
  }
  private val Algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS256

  private case class JwtData(user: String) {
    def toTokenContent: TokenService.Content = TokenService.Content(
      user = user
    )
  }
}

class JwtAuthService extends TokenService {
  override def encodeToken(content: TokenService.Content, expireInSeconds: Long): String = {
    val now = Instant.now

    val claim = JwtClaim(
      content = JwtData(content.user).asJson.toString,
      expiration = Some(now.plusSeconds(expireInSeconds).getEpochSecond),
      issuedAt = Some(now.getEpochSecond)
    )

    JwtCirce.encode(claim, Key, Algorithm)
  }

  override def decodeToken(token: String): Try[TokenService.Token] = {
    for {
      jwtToken <- JwtCirce.decode(token, Key, Seq(Algorithm))
      content <- decode[JwtData](jwtToken.content).toTry
      issuedAt <- jwtToken.issuedAt.toTryOr(new IllegalArgumentException("Missing issuedAt"))
      expiresAt <- jwtToken.expiration.toTryOr(new IllegalArgumentException("Missing expiration"))
    } yield TokenService.Token(
      content = content.toTokenContent,
      issuedAt = Instant.ofEpochSecond(issuedAt),
      expiresAt = Instant.ofEpochSecond(expiresAt)
    )
  }
}
