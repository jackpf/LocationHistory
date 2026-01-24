package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc
import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.enricher.EnricherExecutor
import com.jackpf.locationhistory.server.grpc.interceptors.{AuthenticationInterceptor, TokenService}
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.service.NotificationService
import io.grpc.{ServerInterceptors, ServerServiceDefinition}

import scala.concurrent.ExecutionContext.Implicits.global

object Services {
  def adminServices(
      authenticationManager: AuthenticationManager,
      tokenService: TokenService,
      deviceRepo: DeviceRepo,
      locationRepo: LocationRepo,
      notificationService: NotificationService
  ): Seq[ServerServiceDefinition] = Seq(
    ServerInterceptors.intercept(
      AdminServiceGrpc.bindService(
        new AdminServiceImpl(
          authenticationManager,
          tokenService,
          deviceRepo,
          locationRepo,
          notificationService
        ),
        global
      ),
      new AuthenticationInterceptor(
        tokenService,
        ignoredMethodNames = Set(AdminServiceGrpc.METHOD_LOGIN.getFullMethodName)
      )
    )
  )

  def beaconServices(
      deviceRepo: DeviceRepo,
      locationRepo: LocationRepo,
      enricherExecutor: EnricherExecutor
  ): Seq[ServerServiceDefinition] = Seq(
    BeaconServiceGrpc.bindService(
      new BeaconServiceImpl(deviceRepo, locationRepo, enricherExecutor),
      global
    )
  )
}
