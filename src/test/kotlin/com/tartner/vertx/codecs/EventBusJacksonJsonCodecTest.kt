package com.tartner.vertx.codecs

import com.tartner.vertx.VSerializable
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.vertx.core.buffer.Buffer
import io.vertx.core.buffer.impl.BufferFactoryImpl
import java.util.Date

data class Test1(val id: String, val name: String): VSerializable
data class Test1Array(val test1s: Array<Test1>): VSerializable

data class Test2(val id: String, val address: String, val test1: Test1):
  VSerializable

class EventBusJacksonJsonCodecTest: FeatureSpec() {
  init {
    feature("Encoding") {
      scenario("Encode/Decode a simple class") {
        val mapper = TypedObjectMapper()
        val codec = EventBusJacksonJsonCodec(mapper)

        val buffer: Buffer = BufferFactoryImpl().buffer()!!
        val test1 = Test1("test1id", "Test1")

        codec.encodeToWire(buffer, test1)

        println(buffer)
        val rawDecode = codec.decodeFromWire(0, buffer)
        val test1a: Test1 = rawDecode as Test1
        println(test1a)
        test1a shouldBe test1
      }
      scenario("Encode/Decode a complex class that contains other classes") {
        val mapper = TypedObjectMapper()
        val codec = EventBusJacksonJsonCodec(mapper)

        val buffer: Buffer = BufferFactoryImpl().buffer()!!
        val test1 = Test1("abcdef", "Test1")
        val test2 = Test2("ghijkl", "Test2", test1)

        codec.encodeToWire(buffer, test2)

        println(buffer.toString())

        val test2a = codec.decodeFromWire(0, buffer)
        println(test2a)
        test2a shouldBe test2
      }
    }

    feature("Encoding2") {
      scenario("Encode/Decode a simple class") {
        val codec = EventBusJacksonJsonCodec(TypedObjectMapper())

        val buffer: Buffer = BufferFactoryImpl().buffer()!!
        val tests: List<Test1> = (1..100).map({ Test1("encode2", "Test$it") })
        val test1s = Test1Array(tests.toTypedArray())

        val s1 = Date()
        codec.encodeToWire(buffer, test1s)
        val s2 = Date().time - s1.time
        println("encodeToWire time ${s2}")

//        println(buffer)
        println(buffer.length())

        val s3 = Date()
        val rawDecode = codec.decodeFromWire(0, buffer)
        val s4 = Date().time - s3.time
        println("decodeFromWire time ${s4}")

        val tests1: Test1Array = rawDecode as Test1Array
        tests1.test1s.size shouldBe tests.size
      }
      scenario("Encode/Decode a complex class that contains other classes") {
        val codec = EventBusJacksonJsonCodec(TypedObjectMapper())

        val buffer: Buffer = BufferFactoryImpl().buffer()!!
        val test1 = Test1("t1id", "Test1")
        val test2 = Test2("t2id", "Test2", test1)

        codec.encodeToWire(buffer, test2)

        println(buffer.toString())
        println(buffer.length())

        val test2a = codec.decodeFromWire(0, buffer)
        test2a shouldBe test2
      }
    }
  }
}

