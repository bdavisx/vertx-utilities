package com.tartner.vertx.events

import com.tartner.vertx.OpenForTesting
import com.tartner.vertx.codecs.EventBusJacksonJsonCodec
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus

/** This publishes events in the "standard" way we are doing it for the game engine. */
@OpenForTesting
class EventPublisher(val eventBus: EventBus) {
  val deliveryOptions = DeliveryOptions()
    .setCodecName(EventBusJacksonJsonCodec.codecName)
    .setSendTimeout(5000)

  fun publish(event: Any) {
    eventBus.publish(event::class.qualifiedName, event, deliveryOptions)
  }
}
