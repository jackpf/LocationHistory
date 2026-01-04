package com.jackpf.locationhistory.server.repo

import org.specs2.concurrent.ExecutionEnv

class InMemoryDeviceRepoTest(implicit ee: ExecutionEnv) extends DeviceRepoTest {
  override def createDeviceRepo: DeviceRepo = new InMemoryDeviceRepo()
}
