package com.jackpf.locationhistory.server.grpc.interceptors

import com.jackpf.locationhistory.server.grpc.interceptors.TokenService.{Content, Token}

import java.time.Instant
import scala.util.Try

object TokenService {
  case class Content(
      user: String
  )

  case class Token(
      content: Content,
      issuedAt: Instant,
      expiresAt: Instant
  )
}

trait TokenService {
  def encodeToken(content: Content, expireInSeconds: Long): String
  def decodeToken(token: String): Try[Token]
}
