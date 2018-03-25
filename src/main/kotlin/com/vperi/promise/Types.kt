package com.vperi.promise

typealias SuccessHandlerAsync<V, X> = suspend (V) -> X

typealias FailureHandlerAsync<X> = suspend (Throwable) -> X

typealias Executor<V> = suspend ((V) -> Unit, (Throwable) -> Unit) -> Unit

