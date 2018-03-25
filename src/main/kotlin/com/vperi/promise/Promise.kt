package com.vperi.promise

import com.google.common.collect.ImmutableList
import com.vperi.kotlinx.coroutines.experimental.CoCountdownLatch
import com.vperi.promise.internal.PromiseImpl
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface Promise<V> : Deferred<V> {
  fun <X> then(onResolved: SuccessHandlerAsync<V, X>): Promise<X>

  fun catch(onRejected: FailureHandlerAsync<V>): Promise<V>

  fun <X> then(
    onResolved: SuccessHandlerAsync<V, X>,
    onRejected: FailureHandlerAsync<X>?): Promise<X>

  fun <X> finally(handler: suspend (Result<V>) -> X): Promise<X>

  companion object {

    fun <V> all(items: List<Promise<V>>): Promise<List<V>> {
      val promises = ImmutableList.copyOf(items)
      val size = promises.size
      val results = ConcurrentHashMap<Int, V>()
      val latch = CoCountdownLatch(size.toLong())

      return promise { res, rej ->
        promises.withIndex().forEach { (i, p) ->
          p.then {
            results[i] = it
            latch.countDown()
          }.catch {
            rej(it)
            latch.cancel(it)
          }
        }
        latch.await()
        res((0 until size).map { results[it]!! })
      }
    }
  }
}

fun <V> promise(executor: Executor<V>): Promise<V> {
  val d = PromiseImpl<V>()
  async {
    executor({
      d.complete(it)
    }, {
      d.completeExceptionally(it)
    })
  }
  return d
}

fun Promise.Companion.track(name: String) {
  PromiseImpl.tracker.track(name)
}

fun Promise.Companion.join(name: String) {
  PromiseImpl.tracker.join(name)
}

fun blocking(block: () -> Unit) {
  val name = UUID.randomUUID().toString()
  PromiseImpl.tracker.track(name)
  try {
    block()
  } finally {
    PromiseImpl.tracker.join(name)
  }
}
