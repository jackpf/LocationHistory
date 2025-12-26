package com.jackpf.locationhistory.server.testutil

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

object DefaultSuite {
//  object implicits {
//    extension [A](f: Future[A]) {
//
//      def await(duration: Duration = Duration.Inf): A =
//        Await.result(f, duration)
//    }
//  }
}

abstract class DefaultSuite extends AnyFunSuite with Matchers {}
