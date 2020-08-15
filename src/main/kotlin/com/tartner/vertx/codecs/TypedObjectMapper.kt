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

package com.tartner.vertx.codecs

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tartner.utilities.jackson.ArrowMixIn
import com.tartner.utilities.jackson.JacksonArrowEitherLeftDeserializer
import com.tartner.utilities.jackson.JacksonArrowEitherRightDeserializer
import com.tartner.utilities.jackson.JacksonArrowOptionNoneDeserializer
import com.tartner.utilities.jackson.JacksonArrowOptionSomeDeserializer

/**
Note: This class should only be used for "internal"/trusted serialization/deserialization. There
are security issues around Jackson Data mapper and `activateDefaultTyping...` is part of what allows
the exploit to happen. As long as the json is trusted, this class is safe to use. So there needs to
be another class that is used for stuff coming in over the internet.
 */
class TypedObjectMapper: ObjectMapper() {
  companion object {
    val default = TypedObjectMapper()
  }

  init {
    registerKotlinModule()
    registerModule(JavaTimeModule())
//    enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
    activateDefaultTyping(LaissezFaireSubTypeValidator(), DefaultTyping.NON_FINAL,
      JsonTypeInfo.As.PROPERTY)
    addMixIn(Either::class.java, ArrowMixIn::class.java)
    addMixIn(Option::class.java, ArrowMixIn::class.java)

    // TODO: this belongs somewhere else
    val sm = SimpleModule()
      .addDeserializer(Either.Left::class.java, JacksonArrowEitherLeftDeserializer())
      .addDeserializer(Either.Right::class.java, JacksonArrowEitherRightDeserializer())
      .addDeserializer(Some::class.java, JacksonArrowOptionSomeDeserializer())
      .addDeserializer(None::class.java, JacksonArrowOptionNoneDeserializer())
    registerModule(sm)
  }
}

/** Mapper that s/b used for reading/writing json that goes over the internet. */
class ExternalObjectMapper: ObjectMapper() {
  companion object {
    val default = ExternalObjectMapper()
  }

  init {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    deactivateDefaultTyping()
  }
}
