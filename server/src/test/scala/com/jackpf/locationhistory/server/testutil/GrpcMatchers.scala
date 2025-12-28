package com.jackpf.locationhistory.server.testutil

import io.grpc.{Status, StatusException, StatusRuntimeException}
import org.specs2.execute.ResultImplicits.combineResult
import org.specs2.matcher.Expectations.===
import org.specs2.matcher.MustMatchers.must
import org.specs2.matcher.{Matcher, Matchers}

trait GrpcMatchers extends Matchers {
  def throwAGrpcException(code: Status.Code, messageSubstring: String): Matcher[Any] = {
    throwA[StatusException].like { case e =>
      (e.getStatus.getCode === code) and
        (e.getMessage must contain(messageSubstring))
    }
  }

  def throwAGrpcRuntimeException(code: Status.Code, messageSubstring: String): Matcher[Any] = {
    throwA[StatusRuntimeException].like { case e =>
      (e.getStatus.getCode === code) and
        (e.getMessage must contain(messageSubstring))
    }
  }
}
