package com.jackpf.locationhistory.server.util

import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}
import com.jackpf.locationhistory.server.util.ParamExtractor.*
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ParamExtractorTest(implicit ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    val error: Throwable = new Error("Test error")
  }

  "Param extractor" should {
    "options extractor" >> {
      trait OptionsExtractorContext[T] extends Context {
        def param: Option[T]
      }

      "extract some" >> in(new OptionsExtractorContext[String] {
        override def param: Option[String] = Some("foo")
      }) { context =>
        context.param.toTryOr(context.error) === Success("foo")
        context.param.toFutureOr(context.error) must beEqualTo("foo").await
      }

      "extract none" >> in(new OptionsExtractorContext[String] {
        override def param: Option[String] = None
      }) { context =>
        context.param.toTryOr(context.error) === Failure(context.error)
        context.param.toFutureOr(context.error).failed.map(_.getCause) must beEqualTo(
          context.error
        ).await
      }
    }

    "try extractor" >> {
      trait TryExtractorContext[T] extends Context {
        def param: Try[T]
      }

      "extract success" >> in(new TryExtractorContext[String] {
        override def param: Try[String] = Success("foo")
      }) { context =>
        context.param.toFuture must beEqualTo("foo").await
      }

      "extract failure" >> in(new TryExtractorContext[String] {
        override def param: Try[String] = Failure(error)
      }) { context =>
        context.param.toFuture.failed.map(_.getCause) must beEqualTo(
          context.error
        ).await
      }
    }

    "future option extractor" >> {
      trait FutureOptionExtractorContext[T] extends Context {
        def param: Future[Option[T]]
      }

      "extract some" >> in(new FutureOptionExtractorContext[String] {
        override def param: Future[Option[String]] = Future.successful(Some("foo"))
      }) { context =>
        context.param.toFutureOr(context.error) must beEqualTo("foo").await
      }

      "extract none" >> in(new FutureOptionExtractorContext[String] {
        override def param: Future[Option[String]] = Future.successful(None)
      }) { context =>
        context.param.toFutureOr(context.error).failed.map(_.getCause) must beEqualTo(
          context.error
        ).await
      }

      "propagate future failure" >> in(new FutureOptionExtractorContext[String] {
        override def param: Future[Option[String]] = Future.failed(error)
      }) { context =>
        context.param.toFutureOr(context.error).failed.map(_.getCause) must beEqualTo(
          context.error
        ).await
      }
    }

    "future try extractor" >> {
      trait FutureTryExtractorContext[T] extends Context {
        def param: Future[Try[T]]
      }

      "extract success" >> in(new FutureTryExtractorContext[String] {
        override def param: Future[Try[String]] = Future.successful(Success("foo"))
      }) { context =>
        context.param.toFuture must beEqualTo("foo").await
      }

      "extract failure" >> in(new FutureTryExtractorContext[String] {
        override def param: Future[Try[String]] = Future.successful(Failure(error))
      }) { context =>
        context.param.toFuture.failed.map(_.getCause) must beEqualTo(
          context.error
        ).await
      }

      "propagate future failure" >> in(new FutureTryExtractorContext[String] {
        override def param: Future[Try[String]] = Future.failed(error)
      }) { context =>
        context.param.toFuture.failed.map(_.getCause) must beEqualTo(
          context.error
        ).await
      }
    }
  }
}
