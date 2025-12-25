package com.jackpf.locationhistory.server.grpc

import beacon.beacon_service.BeaconServiceGrpc.BeaconService
import beacon.beacon_service.*
import com.jackpf.locationhistory.server.model.{Device, Location}
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
        deviceRepo.register(Device.fromProto(device)).map { _ =>
          RegisterDeviceResponse()
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
        val status = deviceRepo.get(Device.fromProto(device)).map {
          case Some(storedDevice) => storedDevice.status.toProto
          case None               => DeviceStatus.DEVICE_UNKNOWN
        }

        status.map { s =>
          CheckDeviceResponse(status = s)
        }
      case None =>
        Future.failed(new IllegalArgumentException("No device provided"))
    }
  }

  override def setLocation(
      request: SetLocationRequest
  ): Future[SetLocationResponse] = {
    log.debug(s"Received ping request: ${request}")

    request.device match {
      case Some(device) =>
        deviceRepo.get(Device.fromProto(device)).map {
          case Some(storedDevice) =>
            val location = Location(
              timestamp = System.currentTimeMillis(),
              lat = request.lat,
              lon = request.lon,
              accuracy = request.accuracy
            )

            locationRepo.storeDeviceLocation(storedDevice, location)

            SetLocationResponse(ok = true)
          case None => throw new RuntimeException("Device not found")
        }
      case None =>
        Future.failed(new IllegalArgumentException("No device provided"))
    }
  }
}
