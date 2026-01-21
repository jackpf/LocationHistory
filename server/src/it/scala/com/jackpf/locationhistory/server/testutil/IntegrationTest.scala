package com.jackpf.locationhistory.server.testutil

import com.jackpf.locationhistory.admin_service.AdminServiceGrpc
import com.jackpf.locationhistory.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.enricher.EnricherExecutor
import com.jackpf.locationhistory.server.grpc.interceptors.TokenService
import com.jackpf.locationhistory.server.repo.{
  DeviceRepo,
  InMemoryDeviceRepo,
  InMemoryLocationRepo,
  LocationRepo
}
import com.jackpf.locationhistory.server.service.{JwtAuthService, NotificationService}
import com.jackpf.locationhistory.server.testutil.IntegrationTest.{resetState, startServer}
import io.grpc.stub.MetadataUtils
import io.grpc.{ManagedChannel, ManagedChannelBuilder, Metadata}
import org.mockito.Mockito.mock
import org.specs2.specification.After

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object IntegrationTest {
  val deviceRepo: DeviceRepo = new InMemoryDeviceRepo
  val locationRepo: LocationRepo = new InMemoryLocationRepo
  // TODO Use wiremock
  val notificationService: NotificationService = mock(classOf[NotificationService])
  // TODO Use wiremock
  val enricherExecutor: EnricherExecutor = new EnricherExecutor(Seq.empty)

  def startServer(): Unit = {
    Await.result(
      Future.sequence(
        Seq(deviceRepo.init(), locationRepo.init())
      ),
      Duration.Inf
    ): Unit

    TestServer.start(deviceRepo, locationRepo, notificationService, enricherExecutor)
  }

  def resetState(): Unit = {
    Await.result(
      Future.sequence(
        Seq(deviceRepo.deleteAll(), locationRepo.deleteAll())
      ),
      Duration.Inf
    ): Unit
  }
}

abstract class IntegrationTest extends DefaultSpecification {
  sequential
  startServer()

  trait IntegrationContext extends DefaultScope with After {
    resetState()

    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress("localhost", TestServer.TestPort)
      .usePlaintext()
      .build()

    val client: BeaconServiceGrpc.BeaconServiceBlockingStub =
      BeaconServiceGrpc.blockingStub(channel)

    val tokenService: TokenService = new JwtAuthService
    lazy val tokenDuration: Long = 600L
    lazy val token: String = tokenService.encodeToken(
      TokenService.Content(user = "admin"),
      expireInSeconds = tokenDuration
    )

    val header: Metadata = new Metadata()
    val key: Metadata.Key[String] =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
    header.put(key, s"Bearer ${token}")

    val unauthenticatedAdminClient: AdminServiceGrpc.AdminServiceBlockingStub =
      AdminServiceGrpc.blockingStub(channel)

    val adminClient: AdminServiceGrpc.AdminServiceBlockingStub =
      unauthenticatedAdminClient
        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(header))

    override def after: Any = {
      channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)
    }
  }
}
