package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc.AdminService
import com.jackpf.locationhistory.admin_service.*
import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceNotFoundException,
  InvalidDeviceStatus,
  NoDeviceProvidedException
}
import com.jackpf.locationhistory.server.grpc.ErrorMapper.*
import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.util.Logging

import scala.concurrent.{ExecutionContext, Future}

class AdminServiceImpl(
    deviceRepo: DeviceRepo,
    locationRepo: LocationRepo
)(using ec: ExecutionContext)
    extends AdminService
    with Logging {
  override def listDevices(
      request: ListDevicesRequest
  ): Future[ListDevicesResponse] = {
    for {
      devices <- deviceRepo.getAll
    } yield ListDevicesResponse(devices.map(_.toProto))
  }

  override def approveDevice(
      request: ApproveDeviceRequest
  ): Future[ApproveDeviceResponse] = {
    request.device match {
      case Some(device) =>
        deviceRepo.get(DeviceId(device.id)).flatMap {
          case Some(storedDevice) =>
            if (storedDevice.status == DeviceStatus.Pending) {
              deviceRepo
                .update(storedDevice.register())
                .map(_ => ApproveDeviceResponse(success = true))
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
            Future.failed(DeviceNotFoundException(DeviceId(device.id)).toGrpcError)
        }
      case None =>
        Future.failed(NoDeviceProvidedException().toGrpcError)
    }
  }

  override def listLocations(
      request: ListLocationsRequest
  ): Future[ListLocationsResponse] = {
    request.device match {
      case Some(device) =>
        for {
          locations <- locationRepo.getForDevice(DeviceId(device.id))
        } yield ListLocationsResponse(locations.map(_.toProto))
      case None =>
        Future.failed(NoDeviceProvidedException().toGrpcError)
    }
  }
}
