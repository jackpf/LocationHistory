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
import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.{AroundEach, BeforeAfter}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract class IntegrationTest extends DefaultSpecification with AroundEach {
  private val deviceRepo: DeviceRepo = new InMemoryDeviceRepo
  private val locationRepo: LocationRepo = new InMemoryLocationRepo

  TestServer.start(deviceRepo, locationRepo)

  override def around[R: AsResult](r: => R): Result = {
    Await.result(
      Future.sequence(Seq(deviceRepo.deleteAll(), locationRepo.deleteAll())),
      Duration.Inf
    )

    AsResult(r)
  }

  trait IntegrationContext extends DefaultScope with BeforeAfter {
    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress("localhost", TestServer.TestPort)
      .usePlaintext()
      .build()

    val client: BeaconServiceGrpc.BeaconServiceBlockingStub =
      BeaconServiceGrpc.blockingStub(channel)

    override def before: Any = {
//      Await.result(
//        Future.sequence(Seq(deviceRepo.deleteAll(), locationRepo.deleteAll())),
//        Duration.Inf
//      )
    }

    override def after: Any = {
      println("after")
      channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)
    }
  }
}
