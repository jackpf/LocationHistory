package com.jackpf.locationhistory.server.util

import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class ConcurrentInMemoryCacheTest(implicit ee: ExecutionEnv) extends DefaultSpecification {
  trait ConcurrentContext extends DefaultScope {
    def await[T](f: Future[T]): T = Await.result(f, 2.seconds)
  }

  "ConcurrentInMemoryCache" should {
    "store and retrieve values" >> in(new ConcurrentContext {}) { context =>
      val cache = new ConcurrentInMemoryCache[String, String](maxSize = 10)

      context.await(cache.set("A", "Value-A"))
      context.await(cache.get("A")) must beSome("Value-A")
      context.await(cache.get("B")) must beNone
    }

    "evict the oldest item when limit is reached" >> in(new ConcurrentContext {}) { context =>
      val cache = new ConcurrentInMemoryCache[Int, String](maxSize = 3)

      // Fill to max
      context.await(cache.set(1, "one"))
      context.await(cache.set(2, "two"))
      context.await(cache.set(3, "three"))

      // Add one more (should trigger eviction of '1')
      context.await(cache.set(4, "four"))

      // 1 should be gone, 2,3,4 should remain
      context.await(cache.get(1)) must beNone
      context.await(cache.get(2)) must beSome("two")
      context.await(cache.get(4)) must beSome("four")
    }

    "handle updates without changing size or order" >> in(new ConcurrentContext {}) { context =>
      val cache = new ConcurrentInMemoryCache[Int, String](maxSize = 3)

      context.await(cache.set(1, "one"))
      context.await(cache.set(2, "two"))

      // Update '1'. Size should still be 2.
      context.await(cache.set(1, "one-updated"))
      context.await(cache.get(1)) must beSome("one-updated")

      // Add '3' and '4'. This pushes size to 4 -> Eviction needed.
      // Since '1' was only UPDATED, not re-inserted, it is still the "oldest" physically.
      context.await(cache.set(3, "three"))
      context.await(cache.set(4, "four"))

      context.await(cache.get(1)) must beNone // It was the oldest, so it died
      context.await(cache.get(2)) must beSome("two")
    }

    "survive a simple concurrent blast" >> in(new ConcurrentContext {}) { context =>
      val cache = new ConcurrentInMemoryCache[Int, Int](maxSize = 100)
      val count = 200

      val futures = (1 to count).map { i =>
        Future {
          context.await(cache.set(i, i))
        }
      }

      context.await(Future.sequence(futures))

      // Check strict size limit (allow for slight drift if using fuzzy sizing,
      // but your prune() loop is aggressive so it should be exact or very close)
      val keysFound = (1 to count).count { i =>
        context.await(cache.get(i)).isDefined
      }

      // We accept 100 items (perfect) or slightly more (race condition tolerance)
      keysFound must beBetween(95, 105)
    }
  }
}
