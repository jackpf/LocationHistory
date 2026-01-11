package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.beacon_service.*
import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc.BeaconService
import com.jackpf.locationhistory.common.DeviceStatus
import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  NoDeviceProvidedException,
  NoLocationProvidedException
}
import com.jackpf.locationhistory.server.model.*
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.util.Logging
import com.jackpf.locationhistory.server.util.ParamExtractor.*
import com.jackpf.locationhistory.server.util.ResponseMapper.*

import scala.concurrent.{ExecutionContext, Future}

class BeaconServiceImpl(
    deviceRepo: DeviceRepo,
    locationRepo: LocationRepo
)(using ec: ExecutionContext)
    extends BeaconService
    with Logging {
  override def ping(request: PingRequest): Future[PingResponse] = {
    Future.successful(PingResponse(message = "pong"))
  }

  override def registerDevice(
      request: RegisterDeviceRequest
  ): Future[RegisterDeviceResponse] = {
    for {
      device <- request.device.toFutureOr(NoDeviceProvidedException())
      registerResponse <- deviceRepo.register(Device.fromProto(device))
    } yield registerResponse
  }.toResponse(_ => RegisterDeviceResponse(success = true, status = DeviceStatus.DEVICE_PENDING))

  override def checkDevice(
      request: CheckDeviceRequest
  ): Future[CheckDeviceResponse] = {
    val status = deviceRepo.get(DeviceId(request.deviceId)).map {
      case Some(storedDevice) => storedDevice.status.toProto
      case None               => DeviceStatus.DEVICE_UNKNOWN
    }

    status.map { s =>
      CheckDeviceResponse(status = s)
    }
  }

  override def setLocation(
      request: SetLocationRequest
  ): Future[SetLocationResponse] = {
    for {
      location <- request.location.toFutureOr(NoLocationProvidedException())
      storedDevice <- deviceRepo.getRegisteredDevice(DeviceId(request.deviceId)).toFuture
      storeLocationResponse <- locationRepo
        .storeDeviceLocation(
          storedDevice.device.id,
          Location.fromProto(location),
          timestamp = request.timestamp
        )
    } yield storeLocationResponse
  }.toResponse(_ => SetLocationResponse(success = true))

  override def registerPushHandler(
      request: RegisterPushHandlerRequest
  ): Future[RegisterPushHandlerResponse] = {
    for {
      storedDevice <- deviceRepo.getRegisteredDevice(DeviceId(request.deviceId)).toFuture
      registerHandlerResponse <- deviceRepo
        .update(
          storedDevice.device.id,
          storedDevice =>
            storedDevice.withPushHandler(request.pushHandler.map(PushHandler.fromProto))
        )
    } yield registerHandlerResponse
  }.toResponse(_ => RegisterPushHandlerResponse(success = true))
}
