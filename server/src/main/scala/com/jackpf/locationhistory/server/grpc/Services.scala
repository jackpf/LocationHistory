package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc
import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.grpc.interceptors.AuthenticationInterceptor
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import io.grpc.{ServerInterceptors, ServerServiceDefinition}

import scala.concurrent.ExecutionContext.Implicits.global

object Services {
  def apply(
      adminPassword: String,
      deviceRepo: DeviceRepo,
      locationRepo: LocationRepo
  ): Seq[ServerServiceDefinition] = Seq(
    ServerInterceptors.intercept(
      AdminServiceGrpc.bindService(
        new AdminServiceImpl(deviceRepo, locationRepo),
        global
      ),
      new AuthenticationInterceptor(adminPassword)
    ),
    BeaconServiceGrpc.bindService(
      new BeaconServiceImpl(deviceRepo, locationRepo),
      global
    )
  )
}
