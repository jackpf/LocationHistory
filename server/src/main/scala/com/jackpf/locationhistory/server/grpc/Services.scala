package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc
import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.grpc.interceptors.AuthenticationInterceptor
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import io.grpc.{ServerInterceptors, ServerServiceDefinition}

import scala.concurrent.ExecutionContext.Implicits.global

object Services {
  def adminServices(
      authenticationManager: AuthenticationManager,
      deviceRepo: DeviceRepo,
      locationRepo: LocationRepo
  ): Seq[ServerServiceDefinition] = Seq(
    ServerInterceptors.intercept(
      AdminServiceGrpc.bindService(
        new AdminServiceImpl(authenticationManager, deviceRepo, locationRepo),
        global
      ),
      new AuthenticationInterceptor(
        authenticationManager,
        ignoredMethodNames = Set(AdminServiceGrpc.METHOD_LOGIN.getFullMethodName)
      )
    )
  )

  def beaconServices(
      deviceRepo: DeviceRepo,
      locationRepo: LocationRepo
  ): Seq[ServerServiceDefinition] = Seq(
    BeaconServiceGrpc.bindService(
      new BeaconServiceImpl(deviceRepo, locationRepo),
      global
    )
  )
}
