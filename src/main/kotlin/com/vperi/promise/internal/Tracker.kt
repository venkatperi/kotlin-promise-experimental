package com.vperi.promise.internal

import com.vperi.kotlinx.coroutines.experimental.CoCountingLatch
import kotlinx.coroutines.experimental.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class Tracker {
  private val allPromisesLatch = CoCountingLatch(1)
  private val firstPromise = AtomicBoolean(true)
  private val count = AtomicLong(0)

  fun addRef() {
    if (!firstPromise.getAndSet(false)) {
      allPromisesLatch.countUp()
    }
    count.incrementAndGet()
  }

  fun release() {
    count.decrementAndGet()
    allPromisesLatch.countDown()
  }

  fun join() {
    runBlocking {
      allPromisesLatch.await()
    }
  }
}

class Trackers {
  private val trackers = ConcurrentHashMap<String, Tracker>()

  private fun get(name: String): Tracker = trackers.computeIfAbsent(name, { Tracker() })

  init {
    get("root")
  }

  fun track(name: String? = null): Tracker {
    return get(name ?: UUID.randomUUID().toString())
  }

  fun join(name: String) {
    get(name).join()
    trackers.remove(name)
  }

  /**
   *
   * ConcurrentHashMap's iterator is thread safe.
   */
  fun addRef() =
    trackers.iterator().forEach { (_, v) -> v.addRef() }

  fun release() =
    trackers.iterator().forEach { (_, v) -> v.release() }
}

