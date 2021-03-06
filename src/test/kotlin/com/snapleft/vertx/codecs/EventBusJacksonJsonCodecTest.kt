/*
 * Copyright (c) 2020 Bill Davis.
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

package com.snapleft.vertx.codecs

import com.snapleft.vertx.VSerializable
import io.kotest.matchers.shouldBe
import io.vertx.core.buffer.Buffer
import io.vertx.core.buffer.impl.BufferImpl
import org.junit.Test
import java.util.Date

data class Test1(val id: String, val name: String): VSerializable
data class Test1Array(val test1s: Array<Test1>): VSerializable

data class Test2(val id: String, val address: String, val test1: Test1): VSerializable

class EventBusJacksonJsonCodecTest {
  @Test
  fun encodeDecodeSimpleClass() {
    val mapper = TypedObjectMapper()
    val codec = EventBusJacksonJsonCodec(mapper)

    val buffer: Buffer = BufferImpl.buffer()
    val test1 = Test1("test1id", "Test1")

    codec.encodeToWire(buffer, test1)

    println(buffer)
    val rawDecode = codec.decodeFromWire(0, buffer)
    val test1a: Test1 = rawDecode as Test1
    println(test1a)
    test1a shouldBe test1
  }

  @Test
  fun encodeDecodeAComplexClassThatContainsOtherClasses() {
    val mapper = TypedObjectMapper()
    val codec = EventBusJacksonJsonCodec(mapper)

    val buffer: Buffer = BufferImpl.buffer()
    val test1 = Test1("abcdef", "Test1")
    val test2 = Test2("ghijkl", "Test2", test1)

    codec.encodeToWire(buffer, test2)

    println(buffer.toString())

    val test2a = codec.decodeFromWire(0, buffer)
    println(test2a)
    test2a shouldBe test2
  }

  @Test
  fun encodeDecodeSimpleClass2() {
    val codec = EventBusJacksonJsonCodec(TypedObjectMapper())

    val buffer: Buffer = BufferImpl.buffer()
    val tests: List<Test1> = (1..100).map({ Test1("encode2", "Test$it") })
    val test1s = Test1Array(tests.toTypedArray())

    val s1 = Date()
    codec.encodeToWire(buffer, test1s)
    val s2 = Date().time - s1.time
    println("encodeToWire time $s2")

//        println(buffer)
    println(buffer.length())

    val s3 = Date()
    val rawDecode = codec.decodeFromWire(0, buffer)
    val s4 = Date().time - s3.time
    println("decodeFromWire time $s4")

    val tests1: Test1Array = rawDecode as Test1Array
    tests1.test1s.size shouldBe tests.size
  }

  @Test
  fun encodeDecodeAComplexClassThatContainsOtherClasses2() {
    val codec = EventBusJacksonJsonCodec(TypedObjectMapper())

    val buffer: Buffer = BufferImpl.buffer()
    val test1 = Test1("t1id", "Test1")
    val test2 = Test2("t2id", "Test2", test1)

    codec.encodeToWire(buffer, test2)

    println(buffer.toString())
    println(buffer.length())

    val test2a = codec.decodeFromWire(0, buffer)
    test2a shouldBe test2
  }
}
