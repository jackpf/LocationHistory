package com.jackpf.locationhistory.server.grpc

import beacon.beacon_service.BeaconServiceGrpc.BeaconService
import beacon.beacon_service.{PingRequest, PingResponse}

import scala.concurrent.Future

class BeaconServiceImpl extends BeaconService {
  override def ping(request: PingRequest): Future[PingResponse] = {
    println(s"Received ping request: ${request}")

    Future.successful(PingResponse(ok = true))
  }
}
