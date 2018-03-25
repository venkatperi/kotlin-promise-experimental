package com.vperi.promise

import com.vperi.promise.internal.PromiseImpl

fun <V> Promise.Companion.resolve(value: V): Promise<V> =
  PromiseImpl(value)

fun Promise.Companion.resolve(error: Throwable): Promise<Unit> =
  PromiseImpl(error)

@JvmName("resolveWithType")
fun <V> Promise.Companion.resolve(error: Throwable): Promise<V> =
  PromiseImpl(error)
