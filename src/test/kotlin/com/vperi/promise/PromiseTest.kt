@file:Suppress("RemoveRedundantBackticks")

package com.vperi.promise

import kotlinx.coroutines.experimental.delay
import org.junit.Test
import kotlin.test.assertEquals

class PromiseTest : BaseTest() {

  @Test
  fun promise() {
    Promise.track("promise")
    seq(1)
    val p = promise<Int> { resolve, _ ->
      delay(400)
      resolve(100)
    }

    p.then {
      seq(2)
      println(it)
      "$it:1"
    }.then {
      seq(3)
      delay(100)
      println(it)
    }

    Promise.join("promise")
    finish(4)
  }

  @Test
  fun `wait for all promises to finish`() {
    blocking {
      seq(1)
      val promises = (0 until 1000).map {
        promise<Int> { r, _ ->
          delay(it)
          r(it)
        }
      }
    }
    finish(2)
  }

  @Test
  fun `already resolved promise`() {
    seq(1)
    blocking {
      Promise.resolve(1)
        .then {
          seq(2)
          assertEquals(1, it)
        }
    }
    finish(3)
  }

  @Test
  fun all1() {
    seq(1)
    blocking {
      Promise
        .all(listOf(Promise.resolve(1), Promise.resolve(2), Promise.resolve(3)))
        .then {
          seq(2)
          it.reduce { acc, x -> acc + x }
        }
        .then {
          seq(3)
          assertEquals(6, it)
        }
        .catch {
          expectUnreached()
          println(it)
        }
    }
    finish(4)
  }

}