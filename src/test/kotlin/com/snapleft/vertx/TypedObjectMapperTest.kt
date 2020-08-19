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

package com.snapleft.vertx

import com.fasterxml.jackson.module.kotlin.readValue
import com.snapleft.vertx.codecs.TypedObjectMapper
import org.junit.Test

data class InsideValue(val inside: Int, val string: String): VSerializable
data class TestCommand(val number: Int, val string: String, val insideValue: InsideValue): VCommand

class TypedObjectMapperTest {
  @Test
  fun serializeDeserializeWorks() {
    val serializer = TypedObjectMapper()

    val command = TestCommand(1, "two", InsideValue(11, "insideTwo"))
    val json = serializer.writeValueAsString(command)
    println(json)
    val deserialized = serializer.readValue<TestCommand>(json)
    println(deserialized)
  }
}
