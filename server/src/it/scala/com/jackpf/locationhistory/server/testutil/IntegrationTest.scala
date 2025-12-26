package com.jackpf.locationhistory.server.testutil

import beacon.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.repo.{
  DeviceRepo,
  InMemoryDeviceRepo,
  InMemoryLocationRepo,
  LocationRepo
}
import com.jackpf.locationhistory.server.testutil.{
  DefaultScope,
  DefaultSpecification
}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.specs2.specification.BeforeAfter

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global

abstract class IntegrationTest extends DefaultSpecification {
  private val deviceRepo: DeviceRepo = new InMemoryDeviceRepo
  private val locationRepo: LocationRepo = new InMemoryLocationRepo

  TestServer.start(deviceRepo, locationRepo)

  trait IntegrationContext extends DefaultScope with BeforeAfter {
    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress("localhost", TestServer.TestPort)
      .usePlaintext()
      .build()

    val client: BeaconServiceGrpc.BeaconServiceBlockingStub =
      BeaconServiceGrpc.blockingStub(channel)

    override def before: Any = {
      println("before")
    }

    override def after: Any = {
      println("after")
      channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)
    }
  }
}
