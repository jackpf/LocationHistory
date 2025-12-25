package com.jackpf.locationhistory.server.testutil

import munit.FunSuite

abstract class DefaultSuite extends FunSuite {
  extension [A](actual: A) {

    infix def ===(expected: A): Unit =
      assertEquals(actual, expected)
  }
}
