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
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

interface CoroutineDelegateAutoRegister {
  val commandHandlers: Map<KClass<*>, SuspendableMessageHandler<*>>; get() = emptyMap()
  val commandWithReplyMessageHandlers: Map<KClass<*>, SuspendableReplyMessageHandler<*>>; get() = emptyMap()

  val eventHandlers: Map<KClass<*>, SuspendableMessageHandler<*>>; get() = emptyMap()
  val eventAddressHandlers: Map<String, SuspendableMessageHandler<*>>; get() = emptyMap()

  val routeHandlers: Map<String, SuspendableMessageHandler<HandleSubrouterCallCommand>>; get() = emptyMap()
}

class CoroutineDelegateAutoRegistrar(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender,
  private val eventRegistrar: CommandRegistrar,
  private val routerVerticle: RouterVerticle
) {
  suspend fun registerHandlers(delegate: CoroutineDelegateAutoRegister, scope: CoroutineScope) {
    registerCommandHandlers(delegate, scope)
    registerEventHandlers(delegate, scope)
    registerAPIHandlers(delegate, scope)
  }

  private fun registerCommandHandlers(delegate: CoroutineDelegateAutoRegister, scope: CoroutineScope) {
    delegate.commandHandlers.forEach { (commandClass, handler) ->
      commandRegistrar.registerCommandHandler(scope, commandClass, handler as SuspendableMessageHandler<Any>)
    }

    delegate.commandWithReplyMessageHandlers.forEach { (commandClass, handler) ->
      commandRegistrar.registerCommandHandler(scope, commandClass, handler as SuspendableReplyMessageHandler<Any>)
    }
  }

  private fun registerEventHandlers(delegate: CoroutineDelegateAutoRegister, scope: CoroutineScope) {
    delegate.eventHandlers.forEach { (commandClass, handler) ->
      eventRegistrar.registerCommandHandler(scope, commandClass, handler as SuspendableMessageHandler<Any>)
    }

    delegate.eventAddressHandlers.forEach { (commandClass, handler) ->
      commandRegistrar.registerCommandHandler(scope, commandClass, handler as SuspendableMessageHandler<Any>)
    }
  }

  suspend private fun registerAPIHandlers(delegate: CoroutineDelegateAutoRegister, scope: CoroutineScope) {
    delegate.routeHandlers.forEach { (route, handler) ->
      commandRegistrar.registerCommandHandler(scope, route, handler)
      routerVerticle.addRoute(route, route)
    }
  }
}
