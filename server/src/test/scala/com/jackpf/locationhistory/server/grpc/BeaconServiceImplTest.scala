package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc.BeaconService
import com.jackpf.locationhistory.beacon_service.*
import com.jackpf.locationhistory.common.{Device, DeviceStatus, Location, PushHandler}
import com.jackpf.locationhistory.server.errors.ApplicationErrors.DeviceNotFoundException
import com.jackpf.locationhistory.server.model
import com.jackpf.locationhistory.server.model.{DeviceId, StoredDevice}
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.testutil.{
  DefaultScope,
  DefaultSpecification,
  GrpcMatchers,
  MockModels
}
import io.grpc.Status.Code
import org.mockito.Mockito.{mock, when}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}

class BeaconServiceImplTest(implicit ee: ExecutionEnv)
    extends DefaultSpecification
    with GrpcMatchers {
  trait Context extends DefaultScope {
    val deviceRepo: DeviceRepo = mock(classOf[DeviceRepo])
    val locationRepo: LocationRepo = mock(classOf[LocationRepo])
    val beaconService: BeaconService =
      new BeaconServiceImpl(deviceRepo, locationRepo)
  }

  "Beacon service" should {
    "ping endpoint" >> {
      trait PingContext extends Context {
        lazy val request: PingRequest
        lazy val result: Future[PingResponse] = beaconService.ping(request)
      }

      "call ping endpoint" >> in(new PingContext {
        override lazy val request: PingRequest = PingRequest()
      }) { context =>
        context.result must beEqualTo(PingResponse(message = "pong")).await
      }
    }

    "register device endpoint" >> {
      trait RegisterDeviceContext extends Context {
        lazy val device: Option[Device] = Some(Device(id = "123", name = "dev1", publicKey = "xxx"))
        lazy val expectedDevice: model.Device =
          model.Device(id = DeviceId("123"), name = "dev1", publicKey = "xxx")

        lazy val registerResponse: Future[Try[Unit]]
        when(deviceRepo.register(expectedDevice)).thenReturn(registerResponse)

        lazy val request: RegisterDeviceRequest = RegisterDeviceRequest(device = device)
        lazy val result: Future[RegisterDeviceResponse] = beaconService.registerDevice(request)
      }

      "register a device" >> in(new RegisterDeviceContext {
        override lazy val registerResponse: Future[Try[Unit]] = Future.successful(Success(()))
      }) { context =>
        context.result must beEqualTo(
          RegisterDeviceResponse(success = true, status = DeviceStatus.DEVICE_PENDING)
        ).await
      }

      "fail on empty device" >> in(new RegisterDeviceContext {
        override lazy val device: Option[Device] = None
        override lazy val registerResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.INVALID_ARGUMENT, "No device provided").await
      }

      "propagate errors" >> in(new RegisterDeviceContext {
        override lazy val registerResponse: Future[Try[Unit]] = Future.successful(
          Failure(
            DeviceNotFoundException(DeviceId("123"))
          )
        )
      }) { context =>
        context.result must throwAGrpcException(Code.NOT_FOUND, "Device 123 does not exist").await
      }
    }

    "check device endpoint" >> {
      trait CheckDeviceContext extends Context {
        lazy val device: Option[Device] = Some(Device(id = "123", publicKey = "xxx"))

        lazy val getResponse: Future[Option[StoredDevice]]
        if (device.isDefined) {
          when(deviceRepo.get(DeviceId(device.get.id))).thenReturn(getResponse): Unit
        }

        lazy val request: CheckDeviceRequest = CheckDeviceRequest(device = device)
        lazy val result: Future[CheckDeviceResponse] = beaconService.checkDevice(request)
      }

      "get a device" >> in(new CheckDeviceContext {
        override lazy val getResponse: Future[Option[StoredDevice]] = Future.successful(
          Some(
            MockModels.storedDevice(
              device = model.Device.fromProto(device.get),
              status = StoredDevice.DeviceStatus.Registered
            )
          )
        )
      }) { context =>
        context.result must beEqualTo(
          CheckDeviceResponse(status = DeviceStatus.DEVICE_REGISTERED)
        ).await
      }

      "return unknown status for not-found device" >> in(new CheckDeviceContext {
        override lazy val getResponse: Future[Option[StoredDevice]] = Future.successful(None)
      }) { context =>
        context.result must beEqualTo(
          CheckDeviceResponse(status = DeviceStatus.DEVICE_UNKNOWN)
        ).await
      }

      "fail on empty device" >> in(new CheckDeviceContext {
        override lazy val device: Option[Device] = None
        override lazy val getResponse: Future[Option[StoredDevice]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.INVALID_ARGUMENT, "No device provided").await
      }
    }

    "set location endpoint" >> {
      trait SetLocationContext extends Context {
        lazy val timestamp: Long = 123L
        lazy val device: Option[Device] = Some(Device(id = "123", publicKey = "xxx"))
        lazy val location: Option[Location] = Some(Location(lat = 0.1, lon = 0.2, accuracy = 0.3))

        lazy val getResponse: Future[Option[StoredDevice]]
        if (device.isDefined) {
          when(deviceRepo.get(DeviceId(device.get.id))).thenReturn(getResponse): Unit
        }

        lazy val storeDeviceLocationResponse: Future[Try[Unit]]
        if (device.isDefined && location.isDefined) {
          when(
            locationRepo
              .storeDeviceLocation(
                DeviceId(device.get.id),
                model.Location.fromProto(location.get),
                timestamp
              )
          ).thenReturn(storeDeviceLocationResponse): Unit
        }

        lazy val request: SetLocationRequest =
          SetLocationRequest(timestamp = timestamp, device = device, location = location)
        lazy val result: Future[SetLocationResponse] = beaconService.setLocation(request)
      }

      "set a device location" >> in(new SetLocationContext {
        override lazy val getResponse: Future[Option[StoredDevice]] = Future.successful(
          Some(
            MockModels.storedDevice(
              device = model.Device.fromProto(device.get),
              status = StoredDevice.DeviceStatus.Registered
            )
          )
        )
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] =
          Future.successful(Success(()))
      }) { context =>
        context.result must beEqualTo(SetLocationResponse(success = true)).await
      }

      "fail on empty device" >> in(new SetLocationContext {
        override lazy val device: Option[Device] = None
        override lazy val getResponse: Future[Option[StoredDevice]] = null
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.INVALID_ARGUMENT, "No device provided").await
      }

      "fail on empty location" >> in(new SetLocationContext {
        override lazy val location: Option[Location] = None
        override lazy val getResponse: Future[Option[StoredDevice]] = null
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.INVALID_ARGUMENT, "No location provided").await
      }

      "fail on unregistered device" >> in(new SetLocationContext {
        override lazy val getResponse: Future[Option[StoredDevice]] = Future.successful(
          Some(
            MockModels.storedDevice(
              device = model.Device.fromProto(device.get),
              status = StoredDevice.DeviceStatus.Pending
            )
          )
        )
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(
          Code.PERMISSION_DENIED,
          "Device 123 is not registered"
        ).await
      }

      "fail on missing device" >> in(new SetLocationContext {
        override lazy val getResponse: Future[Option[StoredDevice]] = Future.successful(None)
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(
          Code.NOT_FOUND,
          "Device 123 does not exist"
        ).await
      }

      "propagate errors" >> in(new SetLocationContext {
        override lazy val getResponse: Future[Option[StoredDevice]] = Future.successful(
          Some(
            MockModels.storedDevice(
              device = model.Device.fromProto(device.get),
              status = StoredDevice.DeviceStatus.Registered
            )
          )
        )
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] =
          Future.successful(Failure(DeviceNotFoundException(DeviceId("123"))))
      }) { context =>
        context.result must throwAGrpcException(Code.NOT_FOUND, "Device 123 does not exist").await
      }
    }

    "register push handler endpoint" >> {
      trait RegisterPushHandlerContext extends Context {
        lazy val deviceId = "123"
        lazy val pushHandler: Option[PushHandler] = Some(PushHandler(name = "ph", url = "phUrl"))

        lazy val updateResponse: Future[Try[Unit]] = Future.successful(Success(()))
        when(
          deviceRepo.update(
            eqTo(DeviceId(deviceId)),
            any[model.StoredDevice => model.StoredDevice]()
          )
        )
          .thenReturn(updateResponse)

        lazy val request: RegisterPushHandlerRequest = RegisterPushHandlerRequest(
          deviceId = deviceId,
          pushHandler = pushHandler
        )
        lazy val result: Future[RegisterPushHandlerResponse] =
          beaconService.registerPushHandler(request)
      }

      "register a push handler" >> in(new RegisterPushHandlerContext {}) { context =>
        context.result must beEqualTo(RegisterPushHandlerResponse(success = true)).await
      }

      "not register a push handler if not provided" >> in(new RegisterPushHandlerContext {
        override lazy val pushHandler: Option[PushHandler] = None
      }) { context =>
        context.result must throwAGrpcException(
          Code.INVALID_ARGUMENT,
          "No push handler provided"
        ).await
      }

      "not register a push handler with failure" >> in(new RegisterPushHandlerContext {
        override lazy val updateResponse: Future[Try[Unit]] =
          Future.successful(Failure((DeviceNotFoundException(DeviceId(deviceId)))))
      }) { context =>
        context.result must throwAGrpcException(
          Code.NOT_FOUND,
          "Device 123 does not exist"
        ).await
      }
    }
  }
}
