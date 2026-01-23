package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc.AdminService
import com.jackpf.locationhistory.admin_service.*
import com.jackpf.locationhistory.common.*
import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceNotFoundException,
  InvalidDeviceStatus
}
import com.jackpf.locationhistory.server.grpc.interceptors.TokenService
import com.jackpf.locationhistory.server.model
import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.service.NotificationService
import com.jackpf.locationhistory.notifications.Notification
import com.jackpf.locationhistory.server.testutil.{
  DefaultScope,
  DefaultSpecification,
  GrpcMatchers,
  MockModels
}
import io.grpc.Status.Code
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{mock, when}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import com.jackpf.locationhistory.server.grpc.AdminServiceImpl.DefaultUser
import com.jackpf.locationhistory.server.grpc.AdminServiceImpl.TokenDuration

class AdminServiceImplTest(implicit ee: ExecutionEnv)
    extends DefaultSpecification
    with GrpcMatchers {
  trait Context extends DefaultScope {
    val authenticationManager: AuthenticationManager = mock(classOf[AuthenticationManager])
    val tokenService: TokenService = mock(classOf[TokenService])
    val deviceRepo: DeviceRepo = mock(classOf[DeviceRepo])
    val locationRepo: LocationRepo = mock(classOf[LocationRepo])
    val notificationService: NotificationService = mock(classOf[NotificationService])
    val adminService: AdminService =
      new AdminServiceImpl(
        authenticationManager,
        tokenService,
        deviceRepo,
        locationRepo,
        notificationService
      )
  }

  "Admin service" should {
    "login endpoint" >> {
      trait LoginEndpoint extends Context {
        lazy val password: String = "mock-password"
        lazy val token: String = "mock-token"

        def authenticationResponse: Boolean
        when(authenticationManager.isValidPassword(password)).thenReturn(authenticationResponse)

        when(tokenService.encodeToken(TokenService.Content(DefaultUser), TokenDuration))
          .thenReturn(token)

        val request: LoginRequest = LoginRequest(password = password)
        lazy val result: Future[LoginResponse] = adminService.login(request)
      }

      "return a token on correct password" >> in(new LoginEndpoint {
        override def authenticationResponse: Boolean = true
      }) { context =>
        context.result must beEqualTo(LoginResponse(token = context.token)).await
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

        lazy val getLastLocationsResponse: Future[Map[DeviceId.Type, Option[model.StoredLocation]]]
        getAllResponse.map { devices =>
          when(
            locationRepo.getDevicesLastLocationMap(devices.map(_.device.id))
          ).thenReturn(getLastLocationsResponse)
        }

        val request: ListDevicesRequest = ListDevicesRequest()
        lazy val result: Future[ListDevicesResponse] = adminService.listDevices(request)
      }

      "get all devices" >> in(new ListDevicesContext {
        override lazy val getAllResponse: Future[Seq[model.StoredDevice]] = Future.successful(
          Seq(
            MockModels.storedDevice(
              device = MockModels.device(id = DeviceId("123"), name = "dev1"),
              status = model.StoredDevice.DeviceStatus.Pending,
              pushHandler = None
            ),
            MockModels.storedDevice(
              device = MockModels.device(id = DeviceId("456"), name = "dev2"),
              status = model.StoredDevice.DeviceStatus.Registered,
              pushHandler = Some(MockModels.pushHandler(name = "ph", url = "phUrl"))
            )
          )
        )
        override lazy val getLastLocationsResponse
            : Future[Map[DeviceId.Type, Option[model.StoredLocation]]] =
          Future.successful(
            Map(DeviceId("123") -> None, DeviceId("456") -> Some(MockModels.storedLocation()))
          )
      }) { context =>
        context.result must beEqualTo(
          ListDevicesResponse(devices =
            Seq(
              StoredDeviceWithMetadata(
                storedDevice = Some(
                  StoredDevice(
                    device = Some(Device(id = "123", name = "dev1")),
                    status = DeviceStatus.DEVICE_PENDING,
                    pushHandler = None
                  )
                ),
                lastLocation = None
              ),
              StoredDeviceWithMetadata(
                storedDevice = Some(
                  StoredDevice(
                    device = Some(Device(id = "456", name = "dev2")),
                    status = DeviceStatus.DEVICE_REGISTERED,
                    pushHandler = Some(PushHandler(name = "ph", url = "phUrl"))
                  )
                ),
                lastLocation = Some(MockModels.storedLocation().toProto)
              )
            )
          )
        ).await
      }
    }

    "approve device endpoint" >> {
      trait ApproveDeviceContext extends Context {
        lazy val deviceId: String = "123"

        lazy val getResponse: Future[Try[model.StoredDevice]]
        when(deviceRepo.getPendingDevice(DeviceId(deviceId))).thenReturn(getResponse)

        lazy val updateResponse: Future[Try[Unit]]
        when(
          deviceRepo.update(
            eqTo(DeviceId(deviceId)),
            any[model.StoredDevice => model.StoredDevice]()
          )
        )
          .thenReturn(updateResponse)

        val request: ApproveDeviceRequest = ApproveDeviceRequest(deviceId = deviceId)
        lazy val result: Future[ApproveDeviceResponse] = adminService.approveDevice(request)
      }

      "approve a device" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Try[model.StoredDevice]] =
          Future.successful(
            Success(
              MockModels.storedDevice(
                device = MockModels.device(id = DeviceId("123"), name = "dev1"),
                status = model.StoredDevice.DeviceStatus.Pending
              )
            )
          )
        override lazy val updateResponse: Future[Try[Unit]] = Future.successful(Success(()))
      }) { context =>
        context.result must beEqualTo(ApproveDeviceResponse(success = true)).await
      }

      "fail on device not found" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Try[model.StoredDevice]] =
          Future.successful(Failure(DeviceNotFoundException(DeviceId(deviceId))))
        override lazy val updateResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.NOT_FOUND, "Device 123 does not exist").await
      }

      "fail if device in unknown state" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Try[model.StoredDevice]] =
          Future.successful(
            Failure(
              InvalidDeviceStatus(
                DeviceId(deviceId),
                model.StoredDevice.DeviceStatus.Unknown,
                model.StoredDevice.DeviceStatus.Pending
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

      "propagate update errors" >> in(new ApproveDeviceContext {
        override lazy val getResponse: Future[Try[model.StoredDevice]] = Future.successful(
          Success(
            MockModels.storedDevice(
              device = MockModels.device(id = DeviceId("123"), name = "dev1"),
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
        lazy val deviceId: String = "123"

        lazy val getResponse: Future[Vector[model.StoredLocation]]
        when(locationRepo.getForDevice(DeviceId(deviceId), limit = None)).thenReturn(getResponse)

        val request: ListLocationsRequest = ListLocationsRequest(deviceId = deviceId)
        lazy val result: Future[ListLocationsResponse] = adminService.listLocations(request)
      }

      "list locations for the given device" >> in(new ListLocationsContext {
        override lazy val getResponse: Future[Vector[model.StoredLocation]] = Future.successful(
          Vector(
            MockModels.storedLocation(
              1L,
              MockModels
                .location(lat = 0.1, lon = 0.2, accuracy = 0.3, metadata = Map("k1" -> "v1")),
              startTimestamp = 1L,
              endTimestamp = 2L,
              count = 3L
            ),
            MockModels.storedLocation(
              2L,
              MockModels
                .location(lat = 0.4, lon = 0.5, accuracy = 0.6, metadata = Map("k2" -> "v2")),
              startTimestamp = 2L,
              endTimestamp = 2L,
              count = 1L
            )
          )
        )
      }) { context =>
        context.result must beEqualTo(
          ListLocationsResponse(locations =
            Seq(
              StoredLocation(
                Some(Location(lat = 0.1, lon = 0.2, accuracy = 0.3, metadata = Map("k1" -> "v1"))),
                startTimestamp = 1L,
                endTimestamp = 2L,
                count = 3L
              ),
              StoredLocation(
                Some(Location(lat = 0.4, lon = 0.5, accuracy = 0.6, metadata = Map("k2" -> "v2"))),
                startTimestamp = 2L,
                endTimestamp = 2L,
                count = 1L
              )
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
    }

    "send notification endpoint" >> {
      trait SendNotificationContext extends Context {
        lazy val deviceId: String = "123"
        lazy val notification: Option[Notification] = Some(Notification())
        lazy val expectedNotificationUrl: String

        lazy val getResponse: Future[Try[model.StoredDevice]]
        when(deviceRepo.getRegisteredDevice(DeviceId(deviceId))).thenReturn(getResponse)

        lazy val notificationResponse: Future[Try[Unit]]
        notification.foreach(n =>
          when(notificationService.sendNotification(expectedNotificationUrl, n))
            .thenReturn(notificationResponse)
        )

        val request: SendNotificationRequest =
          SendNotificationRequest(deviceId = deviceId, notification = notification)
        lazy val result: Future[SendNotificationResponse] = adminService.sendNotification(request)
      }

      "send a notification" >> in(new SendNotificationContext {
        override lazy val expectedNotificationUrl: String = MockModels.pushHandler().url
        override lazy val getResponse: Future[Try[model.StoredDevice]] = Future.successful(
          Success(MockModels.storedDevice(pushHandler = Some(MockModels.pushHandler())))
        )
        override lazy val notificationResponse: Future[Try[Unit]] = Future.successful(Success(()))
      }) { context =>
        context.result must beEqualTo(SendNotificationResponse(success = true)).await
      }

      "not send a notification if registered device is not found" >> in(
        new SendNotificationContext {
          override lazy val expectedNotificationUrl: String = null
          override lazy val getResponse: Future[Try[model.StoredDevice]] =
            Future.successful(Failure(DeviceNotFoundException(DeviceId(deviceId))))
          override lazy val notificationResponse: Future[Try[Unit]] = null
        }
      ) { context =>
        context.result must throwAGrpcException(Code.NOT_FOUND, "Device 123 does not exist").await
      }

      "not send an empty notification" >> in(
        new SendNotificationContext {
          override lazy val expectedNotificationUrl: String = null
          override lazy val notification: Option[Notification] = None
          override lazy val getResponse: Future[Try[model.StoredDevice]] = null
          override lazy val notificationResponse: Future[Try[Unit]] = null
        }
      ) { context =>
        context.result must throwAGrpcException(
          Code.INVALID_ARGUMENT,
          "No notification provided"
        ).await
      }

      "not send a notification if push handler is empty" >> in(new SendNotificationContext {
        override lazy val expectedNotificationUrl: String = null
        override lazy val getResponse: Future[Try[model.StoredDevice]] = Future.successful(
          Success(MockModels.storedDevice(pushHandler = None))
        )
        override lazy val notificationResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(
          Code.INVALID_ARGUMENT,
          "Device 123 has no registered push handler"
        ).await
      }

      "propagate errors" >> in(new SendNotificationContext {
        override lazy val expectedNotificationUrl: String = MockModels.pushHandler().url
        override lazy val getResponse: Future[Try[model.StoredDevice]] = Future.successful(
          Success(MockModels.storedDevice(pushHandler = Some(MockModels.pushHandler())))
        )
        override lazy val notificationResponse: Future[Try[Unit]] =
          Future.successful(Failure(new Error("Test error")))
      }) { context =>
        context.result must throwAGrpcException(Code.INTERNAL, "Test error").await
      }
    }
  }
}
