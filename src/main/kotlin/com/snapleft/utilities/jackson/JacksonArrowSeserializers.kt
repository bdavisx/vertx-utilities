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

package com.snapleft.utilities.jackson

import arrow.core.Either
import arrow.core.Some
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
interface ArrowMixIn

class JacksonArrowEitherLeftDeserializer:
  StdDeserializer<Either.Left<Any>>(Either.Left::class.java) {

  override fun deserialize(parser: JsonParser, context: DeserializationContext): Either.Left<Any> {
    val node: JsonNode = parser.codec.readTree(parser)
    val valueNode = node.get("a")
    val mapper: ObjectMapper = parser.codec as ObjectMapper
    val value = mapper.treeToValue<Any>(valueNode, Any::class.java)
    return Either.left(value) as Either.Left<Any>
  }

}

class JacksonArrowEitherRightDeserializer:
  StdDeserializer<Either.Right<Any>>(Either::class.java) {

  override fun deserialize(parser: JsonParser, context: DeserializationContext): Either.Right<Any> {
    val node: JsonNode = parser.codec.readTree(parser)
    val valueNode = node.get("b")
    val mapper: ObjectMapper = parser.codec as ObjectMapper
    val value = mapper.treeToValue<Any>(valueNode, Any::class.java)
    return Either.right(value) as Either.Right<Any>
  }
}

class JacksonArrowOptionSomeDeserializer:
  StdDeserializer<Some<Any>>(Some::class.java) {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): Some<Any> {
    val node: JsonNode = parser.codec.readTree(parser)
    val valueNode = node.get("t")
    val mapper: ObjectMapper = parser.codec as ObjectMapper
    val value = mapper.treeToValue<Any>(valueNode, Any::class.java)
    return Some(value)
  }
}

class JacksonArrowOptionNoneDeserializer:
  StdDeserializer<arrow.core.None>(arrow.core.None::class.java) {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): arrow.core.None {
    return arrow.core.None
  }
}
