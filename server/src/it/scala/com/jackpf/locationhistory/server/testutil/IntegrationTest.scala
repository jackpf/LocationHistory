package com.jackpf.locationhistory.server.testutil

import beacon.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.testutil.{
  DefaultScope,
  DefaultSpecification
}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

abstract class IntegrationTest extends DefaultSpecification {
  trait IntegrationContext extends DefaultScope {
    TestServer.start()

    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress("localhost", TestServer.TestPort)
      .usePlaintext()
      .build()

    val client: BeaconServiceGrpc.BeaconServiceBlockingStub =
      BeaconServiceGrpc.blockingStub(channel)
  }
}
