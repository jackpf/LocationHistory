package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc.AdminService
import com.jackpf.locationhistory.admin_service.{ListDevicesRequest, ListDevicesResponse}
import com.jackpf.locationhistory.common.{Device, DeviceStatus, StoredDevice}
import com.jackpf.locationhistory.server.model
import com.jackpf.locationhistory.server.model.DeviceId
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, GrpcMatchers}
import org.mockito.Mockito.{mock, when}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future

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
  }
}
