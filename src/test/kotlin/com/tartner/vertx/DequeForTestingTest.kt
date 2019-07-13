/*
 * Copyright (c) 2019 Bill Davis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   - http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tartner.vertx

import io.kotlintest.shouldBe
import org.junit.Test

class DequeForTestingTest {
  @Test
  fun emptyTest() {
    val deque = DequeForTesting<Any>()
    deque.isEmpty shouldBe true
  }

  @Test
  fun threeItems() {
    val deque = DequeForTesting<Int>()
    deque.add(1)
    deque.add(2)
    deque.add(3)

    deque.get() shouldBe 1
    deque.get() shouldBe 2
    deque.get() shouldBe 3
  }

  @Test
  fun iterableFold() {
    val deque = DequeForTesting<Int>()
    deque.add(1)
    deque.add(2)
    deque.add(3)

    6 shouldBe deque.fold(0) { r, t -> r + t }
  }

  @Test
  fun emptyToString() {
    val toString = DequeForTesting.Node(1).toString()
    "Node(value=1, next=null, previous=null)" shouldBe toString
  }
}
