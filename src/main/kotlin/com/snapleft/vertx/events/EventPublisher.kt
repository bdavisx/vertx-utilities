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

package com.snapleft.vertx.events

import com.snapleft.vertx.OpenForTesting
import com.snapleft.vertx.codecs.EventBusJacksonJsonCodec
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
