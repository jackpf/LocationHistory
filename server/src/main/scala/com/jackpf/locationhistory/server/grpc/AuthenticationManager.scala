package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.server.grpc.AuthenticationManager.HashFunction

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

object AuthenticationManager {
  private val HashFunction: String = "SHA-256"
}

class AuthenticationManager(adminPassword: String) {
  private val salt: String = UUID.randomUUID.toString

  private def hash(str: String): Array[Byte] = MessageDigest
    .getInstance(HashFunction)
    .digest(salt.getBytes(StandardCharsets.UTF_8) ++ str.getBytes(StandardCharsets.UTF_8))

  private val adminPasswordHashed: Array[Byte] = hash(adminPassword)

  def isValidPassword(password: String): Boolean =
    MessageDigest.isEqual(
      hash(password),
      adminPasswordHashed
    )
}
