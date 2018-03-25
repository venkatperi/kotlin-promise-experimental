package com.vperi.promise.internal

import com.vperi.promise.*
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.async
import java.util.*

class PromiseImpl<V>(parent: PromiseImpl<V>? = null, register: Boolean = true) :
  Promise<V>,
  CompletableDeferred<V> by CompletableDeferred<V>(parent) {

  private val childPromises = WeakHashMap<Promise<*>, Unit>()

  constructor(value: V) : this(register = false) {
    complete(value)
  }

  constructor(error: Throwable) : this(register = false) {
    completeExceptionally(error)
  }

  init {
    if (register) {
      tracker.addRef()
      async {
        join()
        tracker.release()
      }
    }
  }

  override fun <X> then(onResolved: SuccessHandlerAsync<V, X>): Promise<X> =
    then(onResolved, null)

  override fun catch(onRejected: FailureHandlerAsync<V>): Promise<V> =
    then({ it: V -> it }, onRejected)

  override fun <X> then(
    onResolved: SuccessHandlerAsync<V, X>,
    onRejected: FailureHandlerAsync<X>?): Promise<X> {
    val job = promise<X> { resolve, reject ->
      try {
        resolve(onResolved(await()))
      } catch (e: Exception) {
        when {
          onRejected != null -> resolve(onRejected(e.cause ?: e))
          else -> reject(e.cause ?: e)
        }
      }
    }
    addChild(job)
    return job
  }

  override fun <X> finally(handler: suspend (Result<V>) -> X): Promise<X> {
    val job = promise<X> { resolve, _ ->
      try {
        resolve(handler(Result.Value(await())))
      } catch (e: Exception) {
        resolve(handler(Result.Error(e)))
      }
    }
    addChild(job)
    return job
  }

//  override suspend fun join() {
//    childPromises.keys.forEach { it.join() }
//    join()
//  }

  private fun addChild(child: Promise<*>) {
    childPromises[child] = Unit
  }

  companion object {
    var tracker = Trackers()
  }
}

