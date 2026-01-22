package com.jackpf.locationhistory.server.util

import org.slf4j.{Logger, LoggerFactory}
import ch.qos.logback.classic.{Logger as LogbackLogger, Level as LogbackLevel}

object Logging {
  def setLevel(logger: String, level: LogbackLevel): Unit = {
    LoggerFactory
      .getLogger(logger)
      .asInstanceOf[LogbackLogger]
      .setLevel(level)
  }

  def setRootLevel(level: LogbackLevel): Unit =
    setLevel(Logger.ROOT_LOGGER_NAME, level)
}

trait Logging {
  protected val log: Logger = LoggerFactory.getLogger(getClass)
}
