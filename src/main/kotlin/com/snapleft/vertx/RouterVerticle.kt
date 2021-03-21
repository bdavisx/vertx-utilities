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
 */
package com.snapleft.vertx

import com.snapleft.utilities.debugIf
import com.snapleft.vertx.dependencyinjection.PercentOfMaximumVerticleInstancesToDeploy
import com.snapleft.vertx.events.EventPublisher
import com.snapleft.vertx.events.EventRegistrar
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

data class SubrouterAdded(val route: String, val handler: RouteHandler): VEvent
data class SubrouterAddedForMethods(
  val route: String, val methods: List<HttpMethod>, val handler: RouteHandler): VEvent

typealias RouteHandler = suspend (RoutingContext) -> Unit

@PercentOfMaximumVerticleInstancesToDeploy(100)
class RouterVerticle(
  private val eventPublisher: EventPublisher,
  private val eventRegistrar: EventRegistrar,
  private val directCallDelegate: DirectCallDelegate
): CoroutineVerticle() {
  private val log = LoggerFactory.getLogger(RouterVerticle::class.java)
  private val thisAddress = RouterVerticle::class.qualifiedName!!

  private lateinit var mainRouter: Router
  private lateinit var server: HttpServer

  override suspend fun start() {
    super.start()

    directCallDelegate.registerAddress(thisAddress, this)

    eventRegistrar.registerEventHandlerSuspendable(this, SubrouterAdded::class, ::subrouterAdded)
    eventRegistrar.registerEventHandlerSuspendable(this, SubrouterAddedForMethods::class,
      ::subrouterAddedForMethods)

    server = vertx.createHttpServer()
    mainRouter = Router.router(vertx)
    // TODO: parameterize the port
    server.requestHandler(mainRouter).listen(8080).await()
  }

  fun addRoute(route: String, handler: RouteHandler) = directCallDelegate.fireAndForget {
    log.debugIf {"adding route: handler: $handler; route: $route"}
    eventPublisher.publish(SubrouterAdded(route, handler))
  }

  fun addRoute(route: String, method: HttpMethod, handler: RouteHandler) = directCallDelegate.fireAndForget {
    log.debugIf {"adding route: handler: $handler; route: $route; method: $method"}
    eventPublisher.publish(SubrouterAddedForMethods(route, listOf(method), handler))
  }

  fun addRoute(route: String, methods: List<HttpMethod>, handler: RouteHandler) = directCallDelegate.fireAndForget {
    log.debugIf {"adding route: handler: $handler; route: $route; ${methods.joinToString()}"}
    eventPublisher.publish(SubrouterAddedForMethods(route, methods, handler))
  }

  private suspend fun subrouterAdded(event: SubrouterAdded) {
    log.debugIf {"SubrouterAdded event received - $event"}
    val route: Route = mainRouter.route(event.route)
    route.handler { routingContext ->
      launch(vertx.dispatcher()) { event.handler(routingContext) }}
  }

  private suspend fun subrouterAddedForMethods(event: SubrouterAddedForMethods) {
    log.debugIf {"SubrouterAdded event received - $event"}
    val route: Route = mainRouter.route(event.route)
    event.methods.forEach {route.method(it)}

    route.handler { routingContext ->
      launch(vertx.dispatcher()) { event.handler(routingContext) }}
  }
}
