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
import org.specs2.specification.After

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract class IntegrationTest extends DefaultSpecification {
  private val deviceRepo: DeviceRepo = new InMemoryDeviceRepo
  private val locationRepo: LocationRepo = new InMemoryLocationRepo

  TestServer.start(deviceRepo, locationRepo)

  private def resetState(): Unit = {
    Await.result(
      Future.sequence(Seq(deviceRepo.deleteAll(), locationRepo.deleteAll())),
      Duration.Inf
    ): Unit
  }

  trait IntegrationContext extends DefaultScope with After {
    resetState()

    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress("localhost", TestServer.TestPort)
      .usePlaintext()
      .build()

    val client: BeaconServiceGrpc.BeaconServiceBlockingStub =
      BeaconServiceGrpc.blockingStub(channel)

    override def after: Any = {
      channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)
    }
  }
}
