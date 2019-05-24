/*
 * Copyright (c) 2019 the original author or authors.
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

import com.tartner.vertx.MessageHandler
import com.tartner.vertx.OpenForTesting
import com.tartner.vertx.ReplyMessageHandler
import com.tartner.vertx.SuspendableMessageHandler
import com.tartner.vertx.SuspendableReplyMessageHandler
import com.tartner.vertx.VSerializable
import com.tartner.vertx.debugIf
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.logging.LoggerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
Class that handles registering for commands in a standardized way based on the cluster node and
command. The nodeId should be unique within the cluster.
 */
@OpenForTesting
class CommandRegistrar(
  val eventBus: EventBus,
  val commandSender: CommandSender
) {
  private val log = LoggerFactory.getLogger(CommandRegistrar::class.java)

  /** Registers the command with address == commandClass.qualifiedName */
  fun <T: Any> registerCommandHandler(commandClass: KClass<T>, handler: MessageHandler<T>)
    = registerCommandHandler(commandClass.qualifiedName!!, handler)

  fun <T: Any> registerCommandHandler(commandClass: KClass<T>, handler: ReplyMessageHandler<T>)
    = registerCommandHandler(commandClass.qualifiedName!!, handler)

  /** Registers a handler for T that is only local to this node. */
  fun <T: Any> registerCommandHandler(address: String, handler: MessageHandler<T>)
    : MessageConsumer<T> {
    log.debugIf { "Registering command handler for address: $address" }
    return eventBus.localConsumer<T>(address) { message -> handler(message.body()) }
  }

  fun <T: Any> registerCommandHandler(address: String, handler: ReplyMessageHandler<T>)
    : MessageConsumer<T> {
    log.debugIf { "Registering command handler for address: $address" }
    return eventBus.localConsumer<T>(address) {
      message -> handler(message.body()) { commandSender.reply(message, it) }
    }
  }

  fun <T: Any> registerCommandHandler(
    scope: CoroutineScope, commandClass: KClass<T>, handler: SuspendableMessageHandler<T>)
    : MessageConsumer<T> = registerCommandHandler(scope, commandClass.qualifiedName!!, handler)

  fun <T: Any> registerCommandHandler(
    scope: CoroutineScope, commandClass: KClass<T>, handler: SuspendableReplyMessageHandler<T>)
    : MessageConsumer<T> = registerCommandHandler(scope, commandClass.qualifiedName!!, handler)

//  New registration types: gets T and a MessageReply class. or just T or just reply or nothing.
//  Get rid of the existing. For both suspendable and non.

  fun <T: Any> registerCommandHandler(
    scope: CoroutineScope, address: String, handler: SuspendableMessageHandler<T>)
    : MessageConsumer<T> =
      eventBus.localConsumer<T>(address) { message ->
        scope.launch { handler(message.body()) }
      }

  fun <T: Any> registerCommandHandler(
    scope: CoroutineScope, address: String, handler: SuspendableReplyMessageHandler<T>)
    : MessageConsumer<T> =
      eventBus.localConsumer<T>(address) { message ->
        scope.launch { handler(message.body()) { commandSender.reply(message, it) } }
      }
}
