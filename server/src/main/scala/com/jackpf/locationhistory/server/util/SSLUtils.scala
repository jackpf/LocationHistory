package com.jackpf.locationhistory.server.util

import java.io.{File, FileInputStream}
import java.security.MessageDigest
import java.security.cert.{CertificateFactory, X509Certificate}
import scala.util.{Failure, Success, Using}

object SSLUtils {
  def sslFingerprint(certFile: File): String = {
    Using(new FileInputStream(certFile)) { inputStream =>
      val certificateFactory = CertificateFactory.getInstance("X.509")
      val cert = certificateFactory.generateCertificate(inputStream).asInstanceOf[X509Certificate]

      val digest = MessageDigest.getInstance("SHA-256")
      val hash = digest.digest(cert.getEncoded)
      val hex = hash.map("%02X".format(_)).mkString(":")

      s"""${"=" * 50}
         |SERVER FINGERPRINT (SHA-256):
         |${hex}
         |${"=" * 50}""".stripMargin
    }
  } match {
    case Failure(exception) => throw exception
    case Success(v)         => v
  }
}
