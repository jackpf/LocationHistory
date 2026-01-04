package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc.AdminService
import com.jackpf.locationhistory.admin_service.{
  ApproveDeviceRequest,
  ApproveDeviceResponse,
  ListDevicesRequest,
  ListDevicesResponse,
  ListLocationsRequest,
  ListLocationsResponse,
  LoginRequest,
  LoginResponse
}
import com.jackpf.locationhistory.common.{
  Device,
  DeviceStatus,
  Location,
  StoredDevice,
  StoredLocation
}
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
    val authenticationManager: AuthenticationManager = mock(classOf[AuthenticationManager])
    val deviceRepo: DeviceRepo = mock(classOf[DeviceRepo])
    val locationRepo: LocationRepo = mock(classOf[LocationRepo])
    val adminService: AdminService =
      new AdminServiceImpl(authenticationManager, deviceRepo, locationRepo)
  }

  "Admin service" should {
    "login endpoint" >> {
      trait LoginEndpoint extends Context {
        lazy val password: String = "mock-password"
        def authenticationResponse: Boolean
        when(authenticationManager.isValidPassword(password)).thenReturn(authenticationResponse)

        val request: LoginRequest = LoginRequest(password = password)
        val result: Future[LoginResponse] = adminService.login(request)
      }

      "return password as token on correct password" >> in(new LoginEndpoint {
        override def authenticationResponse: Boolean = true
      }) { context =>
        context.result must beEqualTo(LoginResponse(token = context.password)).await
      }

      "return error on incorrect password" >> in(new LoginEndpoint {
        override def authenticationResponse: Boolean = false
      }) { context =>
        context.result must throwAGrpcException(Code.UNAUTHENTICATED, "Invalid password").await
      }
    }

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
          Code.PERMISSION_DENIED,
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
          Code.PERMISSION_DENIED,
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

    "list locations endpoint" >> {
      trait ListLocationsContext extends Context {
        lazy val device: Option[Device] = Some(Device(id = "123", publicKey = "xxx"))

        lazy val getResponse: Future[Vector[model.StoredLocation]]
        if (device.isDefined) {
          when(locationRepo.getForDevice(DeviceId(device.get.id))).thenReturn(getResponse): Unit
        }

        val request: ListLocationsRequest = ListLocationsRequest(device = device)
        val result: Future[ListLocationsResponse] = adminService.listLocations(request)
      }

      "list locations for the given device" >> in(new ListLocationsContext {
        override lazy val getResponse: Future[Vector[model.StoredLocation]] = Future.successful(
          Vector(
            model
              .StoredLocation(model.Location(lat = 0.1, lon = 0.2, accuracy = 0.3), timestamp = 1L),
            model
              .StoredLocation(model.Location(lat = 0.4, lon = 0.5, accuracy = 0.6), timestamp = 2L)
          )
        )
      }) { context =>
        context.result must beEqualTo(
          ListLocationsResponse(locations =
            Seq(
              StoredLocation(Some(Location(lat = 0.1, lon = 0.2, accuracy = 0.3)), timestamp = 1L),
              StoredLocation(Some(Location(lat = 0.4, lon = 0.5, accuracy = 0.6)), timestamp = 2L)
            )
          )
        ).await
      }

      "not fail for an empty list" >> in(new ListLocationsContext {
        override lazy val getResponse: Future[Vector[model.StoredLocation]] = Future.successful(
          Vector.empty
        )
      }) { context =>
        context.result must beEqualTo(ListLocationsResponse(locations = Seq.empty)).await
      }

      "fail on missing device" >> in(new ListLocationsContext {
        override lazy val device: Option[Device] = None
        override lazy val getResponse: Future[Vector[model.StoredLocation]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.INVALID_ARGUMENT, "No device provided").await
      }
    }
  }
}
