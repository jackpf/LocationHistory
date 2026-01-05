package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.common.DeviceStatus
import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc.BeaconService
import com.jackpf.locationhistory.beacon_service.*
import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceNotFoundException,
  DeviceNotRegisteredException,
  NoDeviceProvidedException,
  NoLocationProvidedException
}
import com.jackpf.locationhistory.server.grpc.ErrorMapper.*
import com.jackpf.locationhistory.server.model.{Device, DeviceId, Location, StoredDevice}
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.util.{LocationUtils, Logging}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
    request.device match {
      case Some(device) =>
        deviceRepo.register(Device.fromProto(device)).flatMap {
          case Failure(exception) =>
            Future.failed(exception.toGrpcError)
          case Success(_) =>
            Future.successful(RegisterDeviceResponse(success = true))
        }
      case None =>
        Future.failed(NoDeviceProvidedException().toGrpcError)
    }
  }

  override def checkDevice(
      request: CheckDeviceRequest
  ): Future[CheckDeviceResponse] = {
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
        Future.failed(NoDeviceProvidedException().toGrpcError)
    }
  }

  private def hasPreviousDuplicateLocation(
      deviceId: DeviceId.Type,
      newLocation: Location
  ): Future[Boolean] = {
    for {
      previousLocationMaybe <- locationRepo
        .getForDevice(deviceId, limit = Some(1))
        .map(_.headOption)
    } yield previousLocationMaybe match {
      case Some(previousLocation) =>
        LocationUtils.isDuplicate(previousLocation.location, newLocation, 15)
      case None => false
    }
  }

  override def setLocation(
      request: SetLocationRequest
  ): Future[SetLocationResponse] = {
    if (request.device.isEmpty)
      Future.failed(
        NoDeviceProvidedException().toGrpcError
      )
    else if (request.location.isEmpty)
      Future.failed(NoLocationProvidedException().toGrpcError)
    else {
      val device = request.device.get
      val deviceId = DeviceId(device.id)
      val location = request.location.get
      val newLocation = Location.fromProto(location)

      deviceRepo.get(deviceId).flatMap {
        case Some(storedDevice) =>
          if (storedDevice.status == StoredDevice.DeviceStatus.Registered) {
            {
              for {
                isDuplicate <- hasPreviousDuplicateLocation(deviceId, newLocation)
                setResponse <-
                  if (!isDuplicate)
                    locationRepo
                      .storeDeviceLocation(
                        deviceId,
                        newLocation,
                        timestamp = request.timestamp
                      )
                  else Future.successful(Success(())) // TODO Update timestamp of last location
              } yield setResponse
            }
              .flatMap {
                case Failure(exception) => Future.failed(exception.toGrpcError)
                case Success(_)         => Future.successful(SetLocationResponse(success = true))
              }
          } else {
            Future.failed(DeviceNotRegisteredException(deviceId).toGrpcError)
          }
        case None =>
          Future.failed(
            DeviceNotFoundException(deviceId).toGrpcError
          )
      }
    }
  }
}
