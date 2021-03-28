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

import com.snapleft.vertx.commands.CommandRegistrar
import com.snapleft.vertx.commands.CommandSender
import com.snapleft.vertx.events.EventPublisher
import com.snapleft.vertx.events.EventRegistrar
import com.snapleft.vertx.factories.VerticleInstanceProvider
import com.snapleft.vertx.web.RouterVerticle
import com.snapleft.vertx.web.routerVerticleFactory
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VSerializable::class.java)

suspend fun startRouterVerticles(
  vertx: Vertx,
  deployer: VerticleDeployer,
  instanceProvider: VerticleInstanceProvider,
  eventPublisher: EventPublisher,
  eventRegistrar: EventRegistrar,
  directCallDelegateFactory: DirectCallDelegateFactory,
) {
  val instances = instanceProvider.createInstances(RouterVerticle::class, { routerVerticleFactory(
    eventPublisher, eventRegistrar, directCallDelegateFactory
  )})

  val deploymentPromises = deployer.deployVerticles(vertx, instances)

  val futures: List<Future<VerticleDeployment>> = deploymentPromises.map {it.future()}
  val allFutures = CompositeFuture.all(futures)

  val deployments = futures.map { it: Future<VerticleDeployment> -> it.result()}
}

//////////////////////////////////////////////////////////////////////////////////////////////

data class CommandsAndEventsSystem(
  val commandSender: CommandSender,
  val commandRegistrar: CommandRegistrar,
  val eventPublisher: EventPublisher,
  val eventRegistrar: EventRegistrar,
)

fun createCommandsAndEventsSystem(eventBus: EventBus): CommandsAndEventsSystem {
  val commandSender = CommandSender(eventBus)
  val commandRegistrar = CommandRegistrar(eventBus, commandSender)

  val eventPublisher = EventPublisher(eventBus)
  val eventRegistrar = EventRegistrar(eventBus, commandSender)

  return CommandsAndEventsSystem(commandSender, commandRegistrar, eventPublisher, eventRegistrar)
}
