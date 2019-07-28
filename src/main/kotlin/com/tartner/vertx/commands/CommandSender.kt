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

package com.tartner.vertx.commands

import arrow.core.Either
import com.tartner.vertx.OpenForTesting
import com.tartner.vertx.VSerializable
import com.tartner.vertx.codecs.EventBusJacksonJsonCodec
import com.tartner.vertx.debugIf
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.coroutines.awaitResult

/**
 This evolved from the CommandBus, but once I added a DI library that would work with Verticle's, I
 realized that a class was a better idea. This class will leverage the Vertx EventBus better than a
 CommandBus verticle would, but still has plenty of flexibility.

 At some point (if necessary) this can evolve into a class that does a dynamic service lookup to get
 the address, although we'll probably need to pass in another dependency or change the signature for
 that to work.
 */
@OpenForTesting
class CommandSender(val eventBus: EventBus) {
  private val log = LoggerFactory.getLogger(CommandSender::class.java)

  val deliveryOptions = DeliveryOptions()
    .setCodecName(EventBusJacksonJsonCodec.codecName)
    .setSendTimeout(5000)

  fun send(command: Any) {
    log.debugIf {"Sending command $command to address ${command::class.qualifiedName}"}
    eventBus.send(command::class.qualifiedName, command, deliveryOptions)
  }

  fun <T> send(command: Any, replyHandler: Handler<AsyncResult<Message<T>>>) {
    log.debugIf {"Sending command $command to address ${command::class.qualifiedName} with reply"}
    eventBus.request(command::class.qualifiedName, command, deliveryOptions, replyHandler)
  }

  fun send(address: String, command: Any) {
    log.debugIf {"Sending command $command to $address"}
    eventBus.send(address, command, deliveryOptions)
  }

  fun <T> send(address: String, command: Any, replyHandler: Handler<AsyncResult<Message<T>>>) {
    log.debugIf {"Sending command $command to $address with reply"}
    eventBus.request(address, command, deliveryOptions, replyHandler)
  }

  fun reply(message: Message<*>, reply: Any) {
    log.debugIf {"Replying $reply to ${message.body()}"}
    message.reply(reply, deliveryOptions)
  }

  suspend fun <Failure: VSerializable, Success: VSerializable> sendA(command: Any)
    : Either<Failure, Success>
      = awaitResult<Message<Either<Failure, Success>>> { send(command, it) }.body()
}
