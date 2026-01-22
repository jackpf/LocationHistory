package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.beacon_service.*
import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc.BeaconService
import com.jackpf.locationhistory.common.{Device, DeviceStatus, Location, PushHandler}
import com.jackpf.locationhistory.server.enricher.EnricherExecutor
import com.jackpf.locationhistory.server.errors.ApplicationErrors.DeviceNotFoundException
import com.jackpf.locationhistory.server.model
import com.jackpf.locationhistory.server.model.{DeviceId, StoredDevice}
import com.jackpf.locationhistory.server.repo.LocationRepoExtensions.CheckDuplicateLocationFunc
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class BeaconServiceImplTest(implicit ee: ExecutionEnv)
    extends DefaultSpecification
    with GrpcMatchers {
  trait Context extends DefaultScope {
    val deviceRepo: DeviceRepo = mock(classOf[DeviceRepo])
    val locationRepo: LocationRepo = mock(classOf[LocationRepo])
    val enricherExecutor: EnricherExecutor = mock(classOf[EnricherExecutor])
    val beaconService: BeaconService =
      new BeaconServiceImpl(deviceRepo, locationRepo, enricherExecutor)
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
        lazy val device: Option[Device] = Some(Device(id = "123", name = "dev1"))
        lazy val expectedDevice: model.Device =
          model.Device(id = DeviceId("123"), name = "dev1")

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
        lazy val deviceId: String = "123"

        lazy val getResponse: Future[Option[StoredDevice]]
        when(deviceRepo.get(DeviceId(deviceId))).thenReturn(getResponse)

        lazy val request: CheckDeviceRequest = CheckDeviceRequest(deviceId = deviceId)
        lazy val result: Future[CheckDeviceResponse] = beaconService.checkDevice(request)
      }

      "get a device" >> in(new CheckDeviceContext {
        override lazy val getResponse: Future[Option[StoredDevice]] = Future.successful(
          Some(
            MockModels.storedDevice(
              device = MockModels.device(id = DeviceId(deviceId)),
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
    }

    "set location endpoint" >> {
      trait SetLocationContext extends Context {
        lazy val timestamp: Long = 123L
        lazy val deviceId: String = "123"
        lazy val location: Option[Location] = Some(Location(lat = 0.1, lon = 0.2, accuracy = 0.3))

        lazy val getResponse: Future[Try[StoredDevice]]
        when(deviceRepo.getRegisteredDevice(DeviceId(deviceId))).thenReturn(getResponse)

        lazy val storeDeviceLocationResponse: Future[Try[Unit]]
        lazy val enrichedLocation: model.Location = MockModels.location()
        lazy val enrichedLocationResponse: Future[model.Location] =
          Future.successful(enrichedLocation)

        if (location.isDefined) {
          given ec: ExecutionContext = any[ExecutionContext]()

          when(enricherExecutor.enrich(any[model.Location]())(using any[ExecutionContext]()))
            .thenReturn(enrichedLocationResponse)
          when(
            locationRepo
              .storeDeviceLocationOrUpdatePrevious(
                eqTo(DeviceId(deviceId)),
                eqTo(enrichedLocation),
                eqTo(timestamp),
                any[CheckDuplicateLocationFunc]()
              )
          ).thenReturn(storeDeviceLocationResponse)
        }

        lazy val request: SetLocationRequest =
          SetLocationRequest(timestamp = timestamp, deviceId = deviceId, location = location)
        lazy val result: Future[SetLocationResponse] = beaconService.setLocation(request)
      }

      "set a device location" >> in(new SetLocationContext {
        override lazy val getResponse: Future[Try[StoredDevice]] = Future.successful(
          Success(
            MockModels.storedDevice(
              device = MockModels.device(id = DeviceId(deviceId)),
              status = StoredDevice.DeviceStatus.Registered
            )
          )
        )
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] =
          Future.successful(Success(()))
      }) { context =>
        context.result must beEqualTo(SetLocationResponse(success = true)).await
      }

      "fail on empty location" >> in(new SetLocationContext {
        override lazy val location: Option[Location] = None
        override lazy val getResponse: Future[Try[StoredDevice]] = null
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(Code.INVALID_ARGUMENT, "No location provided").await
      }

      "fail on missing device" >> in(new SetLocationContext {
        override lazy val getResponse: Future[Try[StoredDevice]] =
          Future.successful(Failure(DeviceNotFoundException(DeviceId(deviceId))))
        override lazy val storeDeviceLocationResponse: Future[Try[Unit]] = null
      }) { context =>
        context.result must throwAGrpcException(
          Code.NOT_FOUND,
          "Device 123 does not exist"
        ).await
      }

      "propagate errors" >> in(new SetLocationContext {
        override lazy val getResponse: Future[Try[StoredDevice]] = Future.successful(
          Success(
            MockModels.storedDevice(
              device = MockModels.device(id = DeviceId(deviceId)),
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

        lazy val getResponse: Future[Try[StoredDevice]] =
          Future(
            Success(
              MockModels.storedDevice(
                device = MockModels.device(id = DeviceId(deviceId)),
                status = StoredDevice.DeviceStatus.Registered
              )
            )
          )
        when(deviceRepo.getRegisteredDevice(DeviceId(deviceId))).thenReturn(getResponse)

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

      "un-register a push handler if not provided" >> in(new RegisterPushHandlerContext {
        override lazy val pushHandler: Option[PushHandler] = None
      }) { context =>
        context.result must beEqualTo(RegisterPushHandlerResponse(success = true)).await
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

      "not register a not found device" >> in(new RegisterPushHandlerContext {
        override lazy val getResponse: Future[Try[StoredDevice]] =
          Future(Failure(DeviceNotFoundException(DeviceId(deviceId))))
      }) { context =>
        context.result must throwAGrpcException(
          Code.NOT_FOUND,
          "Device 123 does not exist"
        ).await
      }
    }
  }
}
