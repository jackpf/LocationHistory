package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.*
import com.jackpf.locationhistory.admin_service.AdminServiceGrpc.AdminService
import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceNotFoundException,
  InvalidDeviceStatus,
  InvalidPassword
}
import com.jackpf.locationhistory.server.grpc.ErrorMapper.*
import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.util.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AdminServiceImpl(
    authenticationManager: AuthenticationManager,
    deviceRepo: DeviceRepo,
    locationRepo: LocationRepo
)(using ec: ExecutionContext)
    extends AdminService
    with Logging {
  override def login(request: LoginRequest): Future[LoginResponse] = {
    // TODO Replace with proper tokens
    if (authenticationManager.isValidPassword(request.password))
      Future.successful(LoginResponse(token = request.password))
    else Future.failed(InvalidPassword().toGrpcError)
  }

  override def listDevices(
      request: ListDevicesRequest
  ): Future[ListDevicesResponse] = {
    for {
      devices <- deviceRepo.getAll
    } yield ListDevicesResponse(devices.map(_.toProto))
  }

  override def deleteDevice(request: DeleteDeviceRequest): Future[DeleteDeviceResponse] = {
    val deviceId = DeviceId(request.deviceId)
    for {
      _ <- locationRepo.deleteForDevice(deviceId)
      deleteDevice <- deviceRepo.delete(deviceId)
    } yield {
      DeleteDeviceResponse(success = deleteDevice.isSuccess)
    }
  }

  override def approveDevice(
      request: ApproveDeviceRequest
  ): Future[ApproveDeviceResponse] = {
    deviceRepo.get(DeviceId(request.deviceId)).flatMap {
      case Some(storedDevice) =>
        if (storedDevice.status == DeviceStatus.Pending) {
          deviceRepo
            .update(storedDevice.device.id, device => device.register())
            .flatMap {
              case Failure(exception) => Future.failed(exception.toGrpcError)
              case Success(_)         => Future.successful(ApproveDeviceResponse(success = true))
            }
        } else {
          Future.failed(
            InvalidDeviceStatus(
              storedDevice.device.id,
              actualState = storedDevice.status,
              expectedState = DeviceStatus.Pending
            ).toGrpcError
          )
        }
      case None =>
        Future.failed(DeviceNotFoundException(DeviceId(request.deviceId)).toGrpcError)
    }
  }

  override def listLocations(
      request: ListLocationsRequest
  ): Future[ListLocationsResponse] = {
    for {
      locations <- locationRepo.getForDevice(DeviceId(request.deviceId))
    } yield ListLocationsResponse(locations.map(_.toProto))
  }
}
