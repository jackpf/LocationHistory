package com.jackpf.locationhistory.server.grpc

import beacon.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import io.grpc.ServerServiceDefinition

import scala.concurrent.ExecutionContext.Implicits.global

object Services {
  def apply(
      deviceRepo: DeviceRepo,
      locationRepo: LocationRepo
  ): Seq[ServerServiceDefinition] = Seq(
    BeaconServiceGrpc.bindService(
      new BeaconServiceImpl(deviceRepo, locationRepo),
      global
    )
  )
}
