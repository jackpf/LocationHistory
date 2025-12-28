package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc.AdminService
import com.jackpf.locationhistory.admin_service.{
  ApproveDeviceRequest,
  ApproveDeviceResponse,
  ListDevicesRequest,
  ListDevicesResponse
}
import com.jackpf.locationhistory.common.{Device, DeviceStatus, StoredDevice}
import com.jackpf.locationhistory.server.errors.ApplicationErrors.DeviceNotFoundException
import com.jackpf.locationhistory.server.model
import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, GrpcMatchers}
import io.grpc.Status.Code
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{mock, when}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class AdminServiceImplTest(implicit ee: ExecutionEnv)
    extends DefaultSpecification
    with GrpcMatchers {
  trait Context extends DefaultScope {
    val deviceRepo: DeviceRepo = mock(classOf[DeviceRepo])
    val locationRepo: LocationRepo = mock(classOf[LocationRepo])
    val adminService: AdminService =
      new AdminServiceImpl(deviceRepo, locationRepo)
  }

  "Admin service" should {
    "list devices endpoint" >> {
      trait ListDevicesContext extends Context {
        lazy val getAllResponse: Future[Seq[model.StoredDevice]]
        when(deviceRepo.getAll).thenReturn(getAllResponse)

        val request: ListDevicesRequest = ListDevicesRequest()
        val result: Future[ListDevicesResponse] = adminService.listDevices(request)
      }

      "get all devices" >> in(new ListDevicesContext {
        override lazy val getAllResponse: Future[Seq[model.StoredDevice]] = Future.successful(
          Seq(
            model.StoredDevice(
              device = model.Device(id = DeviceId("123"), publicKey = "xxx"),
              status = model.StoredDevice.DeviceStatus.Pending
            ),
            model.StoredDevice(
              device = model.Device(id = DeviceId("456"), publicKey = "yyy"),
              status = model.StoredDevice.DeviceStatus.Registered
            )
          )
        )
      }) { context =>
        context.result must beEqualTo(
          ListDevicesResponse(devices =
            Seq(
              StoredDevice(
                device = Some(Device(id = "123", publicKey = "xxx")),
                status = DeviceStatus.DEVICE_PENDING
              ),
              StoredDevice(
                device = Some(Device(id = "456", publicKey = "yyy")),
                status = DeviceStatus.DEVICE_REGISTERED
              )
            )
          )
        ).await
      }
    }

    "approve device endpoint" >> {
      trait ApproveDeviceContext extends Context {
        lazy val device: Option[Device] = Some(Device(id = "123", publicKey = "xxx"))

        lazy val getResponse: Future[Option[model.StoredDevice]]
        if (device.isDefined) {
          when(deviceRepo.get(DeviceId(device.get.id))).thenReturn(getResponse): Unit
        }

        lazy val updateResponse: Future[Try[Unit]]
        if (device.isDefined) {
          when(
            deviceRepo.update(
              eqTo(DeviceId(device.get.id)),
              any[model.StoredDevice => model.StoredDevice]()
            )
          )
            .thenReturn(updateResponse): Unit
        }

        val request: ApproveDeviceRequest = ApproveDeviceRequest(device = device)
        val result: Future[ApproveDeviceResponse] = adminService.approveDevice(request)
      }

      "approve a device" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Option[model.StoredDevice]] =
          Future.successful(
            Some(
              model.StoredDevice(
                device = model.Device(id = DeviceId("123"), publicKey = "xxx"),
                status = model.StoredDevice.DeviceStatus.Pending
              )
            )
          )
        override lazy val updateResponse: Future[Try[Unit]] = Future.successful(Success(()))
      }) { context =>
        context.result must beEqualTo(ApproveDeviceResponse(success = true)).await
      }

      "fail on missing device" >> in(new ApproveDeviceContext {
        override lazy val device: Option[Device] = None
        override lazy val getResponse: Future[Option[model.StoredDevice]] = null
        override lazy val updateResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.INVALID_ARGUMENT, "No device provided").await
      }

      "fail on device not found" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Option[model.StoredDevice]] = Future.successful(None)
        override lazy val updateResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.NOT_FOUND, "Device 123 does not exist").await
      }

      "fail if device in unknown state" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Option[model.StoredDevice]] = Future.successful(
          Some(
            model.StoredDevice(
              device = model.Device(id = DeviceId("123"), publicKey = "xxx"),
              status = model.StoredDevice.DeviceStatus.Unknown
            )
          )
        )
        override lazy val updateResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(
          Code.INVALID_ARGUMENT,
          "Device 123 has an invalid state; expected Pending but was Unknown"
        ).await
      }

      "fail if device in registered state" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Option[model.StoredDevice]] = Future.successful(
          Some(
            model.StoredDevice(
              device = model.Device(id = DeviceId("123"), publicKey = "xxx"),
              status = model.StoredDevice.DeviceStatus.Registered
            )
          )
        )
        override lazy val updateResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(
          Code.INVALID_ARGUMENT,
          "Device 123 has an invalid state; expected Pending but was Registered"
        ).await
      }

      "propagate update errors" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Option[model.StoredDevice]] = Future.successful(
          Some(
            model.StoredDevice(
              device = model.Device(id = DeviceId("123"), publicKey = "xxx"),
              status = model.StoredDevice.DeviceStatus.Pending
            )
          )
        )
        override lazy val updateResponse: Future[Try[Unit]] =
          Future.successful(Failure(DeviceNotFoundException(DeviceId("123"))))
      }) { context =>
        context.result must throwAGrpcException(Code.NOT_FOUND, "Device 123 does not exist").await
      }
    }
  }
}
