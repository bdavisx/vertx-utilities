package com.tartner.vertx.codecs

import com.fasterxml.jackson.module.kotlin.readValue
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec

/**
  Note: This class should only be used for "internal"/trusted serialization/deserialization. @see
  TypedObjectMapper for details.

  This class simply uses jackson to serialize/deserialize objects. Jackson s/b setup to store
  classes.
 */
class EventBusJacksonJsonCodec(private val mapper: TypedObjectMapper): MessageCodec<Any, Any> {
  companion object {
    val codecName = EventBusJacksonJsonCodec::class.qualifiedName!!
  }

  override fun systemCodecID(): Byte = -1
  override fun name(): String = codecName
  override fun transform(s: Any): Any = s

  override fun encodeToWire(buffer: Buffer, value: Any) {
    val json = mapper.writeValueAsString(value)
    buffer.appendInt(json.length)
    buffer.appendString(json)
  }

  override fun decodeFromWire(initialPosition: Int, buffer: Buffer): Any {
    val position = initialPosition
    val size = buffer.getInt(position)
    val jsonPosition = position + 4
    val json = buffer.getString(jsonPosition, jsonPosition + size)

    // don't see a way around the unchecked cast, since we lose the T type at runtime
    @Suppress("UNCHECKED_CAST") return mapper.readValue<Any>(json)
  }
}
