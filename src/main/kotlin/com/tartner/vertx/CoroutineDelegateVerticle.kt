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
import com.tartner.vertx.events.EventRegistrar
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.CoroutineScope

interface CoroutineDelegate

interface CommandHandlingCoroutineDelegate: CoroutineDelegate {
  suspend fun registerCommands(scope: CoroutineScope, commandRegistrar: CommandRegistrar)
}

interface EventHandlingCoroutineDelegate: CoroutineDelegate {
  suspend fun registerEvents(scope: CoroutineScope, eventRegistrar: EventRegistrar)
}

typealias RouteRegistrar =
  suspend (route: String, handler: SuspendableMessageHandler<HandleSubrouterCallCommand>) -> Unit

interface APICoroutineDelegate: CoroutineDelegate {
  suspend fun registerRoutes(scope: CoroutineScope, routeRegistrar: RouteRegistrar)
}

class CoroutineDelegateVerticleFactory(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender,
  private val eventRegistrar: EventRegistrar,
  private val routerVerticle: RouterVerticle
) {
  fun create(delegate: CoroutineDelegate) =
    CoroutineDelegateVerticle(commandRegistrar, commandSender, eventRegistrar, routerVerticle,
      delegate)
}

/**
 A verticle that is designed to take an object and delegate command and/or event handling to that
 object, while running in the vertx event loop system.
 */
class CoroutineDelegateVerticle(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender,
  private val eventRegistrar: EventRegistrar,
  private val routerVerticle: RouterVerticle,
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

  private suspend fun registerCommandHandlers(delegate: CommandHandlingCoroutineDelegate) {
    delegate.registerCommands(this, commandRegistrar)
  }

  private suspend fun registerEventHandlers(delegate: EventHandlingCoroutineDelegate) {
    delegate.registerEvents(this, eventRegistrar)
  }

  private suspend fun registerAPIHandlers(delegate: APICoroutineDelegate) {
    delegate.registerRoutes(this, ::registerRoute)
  }

  suspend private fun registerRoute(route: String,
    handler: SuspendableMessageHandler<HandleSubrouterCallCommand>) {
    commandRegistrar.registerCommandHandler(this, route, handler)
    routerVerticle.addRoute(AddRouteCommand(route, route))
  }
}
