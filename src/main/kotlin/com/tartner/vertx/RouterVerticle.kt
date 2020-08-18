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
 */
package com.tartner.vertx

import com.tartner.utilities.debugIf
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.events.EventPublisher
import com.tartner.vertx.events.EventRegistrar
import com.tartner.vertx.kodein.PercentOfMaximumVerticleInstancesToDeploy
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

/** An address will be registered that takes a RoutingContext message and can do whatever with it. */
data class AddRouteCommand(val handlerAddress: String, val route: String): VCommand
data class HandleSubrouterCallCommand(val routingContext: RoutingContext): VCommand
data class SubrouterAdded(val handlerAddress: String, val path: String): VEvent

@PercentOfMaximumVerticleInstancesToDeploy(100)
class RouterVerticle(
  private val commandSender: CommandSender,
  private val eventPublisher: EventPublisher,
  private val eventRegistrar: EventRegistrar
): DirectCallVerticle<RouterVerticle>(RouterVerticle::class.qualifiedName!!) {
  private val log = LoggerFactory.getLogger(RouterVerticle::class.java)

  private lateinit var mainRouter: Router
  private lateinit var server: HttpServer

  override suspend fun start() {
    super.start()

    eventRegistrar.registerEventHandler(SubrouterAdded::class, ::subrouterAdded)

    server = vertx.createHttpServer()
    mainRouter = Router.router(vertx)
    // TODO: parameterize the port
    server.requestHandler(mainRouter).listen(8080)
  }

  suspend fun addRoute(command: AddRouteCommand) = fireAndForget {
    val (handlerAddress, route) = command
    log.debugIf {"adding route: address: $handlerAddress; route: $route"}
    eventPublisher.publish(SubrouterAdded(handlerAddress, route))
  }

  private fun subrouterAdded(event: SubrouterAdded) {
    log.debugIf {"SubrouterAdded event received - $event"}
    val route: Route = mainRouter.route().path(event.path)
    route.handler { routingContext ->
      commandSender.send(event.handlerAddress, HandleSubrouterCallCommand(routingContext))
    }
  }
}
