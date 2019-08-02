/*
 * Copyright (c) 2019, Bill Davis.
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
 */

package com.tartner.vertx

import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.commands.CommandSender
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlin.reflect.KClass

interface CoroutineDelegate

interface CommandHandlingCoroutineDelegate: CoroutineDelegate {
  val commandHandlers: Map<KClass<*>, SuspendableMessageHandler<*>>; get() = emptyMap()
  val commandWithReplyMessageHandlers: Map<KClass<*>, SuspendableMessageHandler<*>>; get() = emptyMap()
}

interface EventHandlingCoroutineDelegate: CoroutineDelegate {
  val eventHandlers: Map<KClass<*>, SuspendableMessageHandler<*>>; get() = emptyMap()
  val eventAddressHandlers: Map<String, SuspendableMessageHandler<*>>; get() = emptyMap()
}

interface APICoroutineDelegate: CoroutineDelegate {
  val routeHandlers: Map<String, SuspendableMessageHandler<HandleSubrouterCallCommand>>; get() = emptyMap()
}

class CoroutineDelegateVerticleFactory(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender,
  private val eventRegistrar: CommandRegistrar
) {
  fun create(delegate: CoroutineDelegate) =
    CoroutineDelegateVerticle(commandRegistrar, commandSender, eventRegistrar, delegate)
}

/**
 A verticle that is designed to take an object and delegate command and/or event handling to that
 object, while running in the vertx event loop system.
 */
class CoroutineDelegateVerticle(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender,
  private val eventRegistrar: CommandRegistrar,
  private val delegate: CoroutineDelegate
): CoroutineVerticle() {
  override suspend fun start() {
    if (delegate is CommandHandlingCoroutineDelegate) {
      registerCommandHandlers(delegate)
    }

    if (delegate is EventHandlingCoroutineDelegate) {
      registerEventHandlers(delegate)
    }

    if (delegate is APICoroutineDelegate) {
      registerAPIHandlers(delegate)
    }
  }

  private fun registerCommandHandlers(delegate: CommandHandlingCoroutineDelegate) {
    delegate.commandHandlers.forEach { (commandClass, handler) ->
      commandRegistrar.registerCommandHandler(this, commandClass, handler as SuspendableMessageHandler<Any>)
    }

    delegate.commandWithReplyMessageHandlers.forEach { (commandClass, handler) ->
      commandRegistrar.registerCommandHandler(this, commandClass, handler as SuspendableReplyMessageHandler<Any>)
    }
  }

  private fun registerEventHandlers(delegate: EventHandlingCoroutineDelegate) {
    delegate.eventHandlers.forEach { (commandClass, handler) ->
      eventRegistrar.registerCommandHandler(this, commandClass, handler as SuspendableMessageHandler<Any>)
    }

    delegate.eventAddressHandlers.forEach { (commandClass, handler) ->
      commandRegistrar.registerCommandHandler(this, commandClass, handler as SuspendableMessageHandler<Any>)
    }
  }

  private fun registerAPIHandlers(delegate: APICoroutineDelegate) {
    delegate.routeHandlers.forEach { (route, handler) ->
      commandRegistrar.registerCommandHandler(this, route, handler)
      commandSender.send(AddRouteCommand(route, route))
    }
  }
}
