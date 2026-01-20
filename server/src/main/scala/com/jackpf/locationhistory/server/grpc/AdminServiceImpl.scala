package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.*
import com.jackpf.locationhistory.admin_service.AdminServiceGrpc.AdminService
import com.jackpf.locationhistory.server.errors.ApplicationErrors.*
import com.jackpf.locationhistory.server.grpc.AdminServiceImpl.{DefaultUser, TokenDuration}
import com.jackpf.locationhistory.server.grpc.interceptors.TokenService
import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.service.NotificationService
import com.jackpf.locationhistory.server.util.Logging
import com.jackpf.locationhistory.server.util.ParamExtractor.*
import com.jackpf.locationhistory.server.util.ResponseMapper.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AdminServiceImpl {
  val DefaultUser: String = "admin"
  val TokenDuration: Long = 3600
}

class AdminServiceImpl(
    authenticationManager: AuthenticationManager,
    tokenService: TokenService,
    deviceRepo: DeviceRepo,
    locationRepo: LocationRepo,
    notificationService: NotificationService
)(using ec: ExecutionContext)
    extends AdminService
    with Logging {
  override def login(request: LoginRequest): Future[LoginResponse] = {
    Future {
      if (authenticationManager.isValidPassword(request.password)) Success(())
      else Failure(InvalidPassword())
    }
  }.toResponse(_ =>
    LoginResponse(token =
      tokenService.encodeToken(
        TokenService.Content(user = DefaultUser),
        expireInSeconds = TokenDuration
      )
    )
  )

  override def listDevices(
      request: ListDevicesRequest
  ): Future[ListDevicesResponse] = {
    for {
      devices <- deviceRepo.getAll
      lastLocationsMap <- locationRepo.getDevicesLastLocationMap(devices.map(_.device.id))
    } yield ListDevicesResponse(devices.map { storedDevice =>
      StoredDeviceWithMetadata(
        storedDevice = Some(storedDevice.toProto),
        lastLocation = lastLocationsMap.get(storedDevice.device.id).flatten.map(_.toProto)
      )
    })
  }

  override def deleteDevice(request: DeleteDeviceRequest): Future[DeleteDeviceResponse] = {
    val deviceId = DeviceId(request.deviceId)
    for {
      _ <- locationRepo.deleteForDevice(deviceId)
      deleteDevice <- deviceRepo.delete(deviceId)
    } yield deleteDevice
  }.toResponse(_ => DeleteDeviceResponse(success = true))

  override def approveDevice(
      request: ApproveDeviceRequest
  ): Future[ApproveDeviceResponse] = {
    val deviceId = DeviceId(request.deviceId)

    for {
      storedDevice <- deviceRepo.getPendingDevice(deviceId).toFuture
      response <- deviceRepo.update(storedDevice.device.id, device => device.register())
    } yield response
  }.toResponse(_ => ApproveDeviceResponse(success = true))

  override def listLocations(
      request: ListLocationsRequest
  ): Future[ListLocationsResponse] = {
    for {
      locations <- locationRepo.getForDevice(DeviceId(request.deviceId), limit = None)
    } yield ListLocationsResponse(locations.map(_.toProto))
  }

  override def sendNotification(
      request: SendNotificationRequest
  ): Future[SendNotificationResponse] = {
    val deviceId = DeviceId(request.deviceId)

    for {
      notification <- request.notification.toFutureOr(NoNotificationProvided())
      storedDevice <- deviceRepo.getRegisteredDevice(deviceId).toFuture
      pushHandler <- storedDevice.pushHandler.toFutureOr(NoPushHandler(deviceId))
      response <- notificationService.sendNotification(pushHandler.url, notification)
    } yield response
  }.toResponse(_ => SendNotificationResponse(success = true))
}
