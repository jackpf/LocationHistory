package com.jackpf.locationhistory.server

case class Args(
    listenPort: Option[Int] = None,
    adminPassword: Option[String] = None
)
