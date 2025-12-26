package com.jackpf.locationhistory.server.grpc

import beacon.beacon_service.*
import beacon.beacon_service.BeaconServiceGrpc.BeaconService
import com.jackpf.locationhistory.server.model.{Device, DeviceId, Location}
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.util.Logging

import scala.concurrent.{ExecutionContext, Future}

class BeaconServiceImpl(
    deviceRepo: DeviceRepo,
    locationRepo: LocationRepo
)(using ec: ExecutionContext)
    extends BeaconService
    with Logging {
  override def ping(request: PingRequest): Future[PingResponse] = {
    log.debug(s"Received ping request: ${request}")

    Future.successful(PingResponse(message = "pong"))
  }

  override def registerDevice(
      request: RegisterDeviceRequest
  ): Future[RegisterDeviceResponse] = {
    log.debug(s"Received registerDevice request: ${request}")

    request.device match {
      case Some(device) =>
        deviceRepo.register(Device.fromProto(device)).flatMap {
          case Left(status) => Future.failed(status.asRuntimeException())
          case Right(value) => Future.successful(RegisterDeviceResponse())
        }
      case None =>
        Future.failed(new IllegalArgumentException("No device provided"))
    }
  }

  override def checkDevice(
      request: CheckDeviceRequest
  ): Future[CheckDeviceResponse] = {
    log.debug(s"Received checkDevice request: ${request}")

    request.device match {
      case Some(device) =>
        val status = deviceRepo.get(DeviceId(device.id)).map {
          case Some(storedDevice) => storedDevice.status.toProto
          case None               => DeviceStatus.DEVICE_UNKNOWN
        }

        status.map { s =>
          CheckDeviceResponse(status = s)
        }
      case None =>
        Future.failed(Errors.noDeviceProvided().asRuntimeException())
    }
  }

  override def setLocation(
      request: SetLocationRequest
  ): Future[SetLocationResponse] = {
    log.debug(s"Received ping request: ${request}")

    (request.device, request.location) match {
      case (Some(device), Some(location)) =>
        deviceRepo.get(DeviceId(device.id)).map {
          case Some(storedDevice) =>
            // TODO Add helper method(s)
            val locationRequest = Location(
              timestamp = System.currentTimeMillis(),
              lat = location.lat,
              lon = location.lon,
              accuracy = location.accuracy
            )

            locationRepo.storeDeviceLocation(storedDevice, locationRequest)

            SetLocationResponse(ok = true)
          case None => throw new IllegalArgumentException("Device not found")
        }
      case _ =>
        // TODO Test & improve error handling/exception throwing
        Future.failed(
          new IllegalArgumentException("No device and/or location provided")
        )
    }
  }
}
