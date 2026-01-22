package com.jackpf.locationhistory.server.util

import com.jackpf.locationhistory.server.util.Cache.Cacheable

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import scala.annotation.tailrec
import scala.collection.concurrent
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

object Cache {
  trait Cacheable[C[_]] {
    def wrap[V](value: V): C[V]
    def unwrap[V](value: C[V]): Option[V]
  }

  trait LowPriorityCacheable {
    private type Identity[x] = x

    implicit val anyCacheable: Cacheable[Identity] = new Cacheable[Identity] {
      override def wrap[V](v: V): Identity[V] = v
      override def unwrap[V](v: Identity[V]): Option[V] = Some(v)
    }
  }

  object Cacheable extends LowPriorityCacheable {
    implicit val optionCacheable: Cacheable[Option] = new Cacheable[Option] {
      override def wrap[V](v: V): Option[V] = Some(v)
      override def unwrap[V](v: Option[V]): Option[V] = v
    }

    implicit val tryCacheable: Cacheable[Try] = new Cacheable[Try] {
      override def wrap[V](v: V): Try[V] = Success(v)
      override def unwrap[V](v: Try[V]): Option[V] = v.toOption
    }
  }
}

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

  def getOrElse[C[_]](
      key: K,
      orElse: => Future[C[V]]
  )(using ec: ExecutionContext, conv: Cacheable[C]): Future[C[V]] = {
    get(key).flatMap {
      case Some(value) => Future.successful(conv.wrap(value))
      case None        =>
        val result = orElse
        result.onComplete(t => t.toOption.flatMap(conv.unwrap).foreach(set(key, _)))
        result
    }
  }
}
