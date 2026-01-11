package com.jackpf.locationhistory.server.util

import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}
import com.jackpf.locationhistory.server.util.ResponseMapper.*
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ResponseMapperTest(implicit ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    val error: Throwable = new Error("Test error")
  }

  "Response mapper" should {
    trait ResponseMapperContext[T] extends Context {
      def response: Future[Try[T]]
    }

    "map success" >> in(new ResponseMapperContext[String] {
      override def response: Future[Try[String]] = Future.successful(Success("foo"))
    }) { context =>
      context.response.toResponse(_.toUpperCase) must beEqualTo("FOO").await
    }

    "map failure" >> in(new ResponseMapperContext[String] {
      override def response: Future[Try[String]] = Future.successful(Failure(error))
    }) { context =>
      context.response.toResponse(identity).failed.map(_.getCause) must beEqualTo(
        context.error
      ).await
    }
  }
}
