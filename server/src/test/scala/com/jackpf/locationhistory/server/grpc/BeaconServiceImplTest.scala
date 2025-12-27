package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc.BeaconService
import com.jackpf.locationhistory.beacon_service.{PingRequest, PingResponse}
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}
import org.mockito.Mockito.mock
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future

class BeaconServiceImplTest(implicit ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    val deviceRepo: DeviceRepo = mock(classOf[DeviceRepo])
    val locationRepo: LocationRepo = mock(classOf[LocationRepo])
    val beaconService: BeaconService =
      new BeaconServiceImpl(deviceRepo, locationRepo)
  }

  trait PingContext extends Context {
    lazy val request: PingRequest
    lazy val result: Future[PingResponse] = beaconService.ping(request)
  }

  "Beacon service" should {
    "call ping endpoint" >> {
      val context: PingContext = new PingContext {
        override lazy val request: PingRequest = PingRequest()
      }

      context.result.map(_.message) must be("pong").await
    }
  }
}
