package com.jackpf.locationhistory.server.util

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import scala.annotation.tailrec
import scala.collection.concurrent
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

trait Cache[K, V] {
  def set(key: K, value: V): Future[Unit]

  def get(key: K): Future[Option[V]]
}

class ConcurrentInMemoryCache[K, V](maxSize: Long) extends Cache[K, V] {
  private val cache: concurrent.Map[K, V] = concurrent.TrieMap.empty
  private val inserts: ConcurrentLinkedQueue[K] = new ConcurrentLinkedQueue()
  private val size: AtomicLong = new AtomicLong()

  @tailrec
  private def prune(): Unit = {
    if (size.get() > maxSize) {
      val polled = inserts.poll()

      if (polled != null) {
        if (cache.remove(polled).isDefined) size.decrementAndGet(): Unit
        prune()
      }
    }
  }

  override def set(key: K, value: V): Future[Unit] = Future.successful {
    if (cache.putIfAbsent(key, value).isEmpty) {
      size.incrementAndGet()
      inserts.add(key)
    } else {
      cache.replace(key, value): Unit
    }

    prune()
  }

  override def get(key: K): Future[Option[V]] = Future.successful {
    cache.get(key)
  }

  def getOrElse(key: K, orElse: => Future[V])(using ec: ExecutionContext): Future[V] = {
    getOrElseTry(key, orElse.transform(Success.apply)).flatMap(Future.fromTry)
  }

  def getOrElseTry(
      key: K,
      orElse: => Future[Try[V]]
  )(using ec: ExecutionContext): Future[Try[V]] = {
    get(key).flatMap {
      case Some(value) => Future.successful(Success(value))
      case None        =>
        val result = orElse
        result.onComplete(t => t.flatten.foreach(value => set(key, value)))
        result
    }
  }
}
