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

package com.snapleft.vertx.commands

import com.snapleft.utilities.debugIf
import com.snapleft.vertx.MessageHandler
import com.snapleft.vertx.OpenForTesting
import com.snapleft.vertx.ReplyMessageHandler
import com.snapleft.vertx.SuspendableMessageHandler
import com.snapleft.vertx.SuspendableReplyMessageHandler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
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
    log.debugIf { "Registering command handler ($handler) for address: $address" }
    return eventBus.localConsumer(address) { message -> handler(message.body()) }
  }

  fun <T: Any> registerCommandHandler(address: String, handler: ReplyMessageHandler<T>)
    : MessageConsumer<T> {
    log.debugIf { "Registering command handler ($handler) for address: $address" }
    return eventBus.localConsumer(address) {
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

  fun <T: Any> registerCommandHandler(scope: CoroutineScope, address: String,
    handler: SuspendableMessageHandler<T>): MessageConsumer<T> {
      log.debugIf {"Registering coroutine command handler ($handler) for address $address in scope $scope"}
      return eventBus.localConsumer(address) { message ->
        scope.launch { handler(message.body()) }
      }
  }

  fun <T: Any> registerCommandHandler(scope: CoroutineScope, address: String,
    handler: SuspendableReplyMessageHandler<T>): MessageConsumer<T> {
    log.debugIf {"Registering coroutine command handler ($handler) for address $address in scope $scope"}
    return eventBus.localConsumer(address) { message ->
      scope.launch { handler(message.body()) { commandSender.reply(message, it) } }
    }
  }
}
