package com.tartner.vertx

import com.fasterxml.jackson.module.kotlin.readValue
import com.tartner.vertx.codecs.TypedObjectMapper
import io.kotlintest.specs.FeatureSpec

data class InsideValue(val inside: Int, val string: String): VSerializable
data class TestCommand(val number: Int, val string: String, val insideValue: InsideValue): VCommand

class TypedObjectMapperTest: FeatureSpec() {
  init {
    feature("Serialize/Deserialize functions") {
      scenario("Serialize/Deserialize works") {
        val serializer = TypedObjectMapper()

        val command = TestCommand(1, "two", InsideValue(11, "insideTwo"))
        val json = serializer.writeValueAsString(command)
        println(json)
        val deserialized = serializer.readValue<TestCommand>(json)
        println(deserialized)
      }
    }
  }
}

