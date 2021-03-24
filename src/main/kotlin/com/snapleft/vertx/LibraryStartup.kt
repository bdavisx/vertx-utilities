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
import com.snapleft.vertx.codecs.PassThroughCodec
import com.snapleft.vertx.commands.CommandRegistrar
import com.snapleft.vertx.commands.CommandSender
import com.snapleft.vertx.cqrs.eventsourcing.EventSourcingApiVerticle
import com.snapleft.vertx.events.EventPublisher
import com.snapleft.vertx.events.EventRegistrar
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.kodein.di.DirectDI
import org.kodein.di.TT
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/*
Putting all the different startups in here so there's only one place you need to go to start the
different pieces of the library. Not sure if that's the best design or not, but there's nothing
stopping someone from doing their own startup in a different way.
*/

private val log = LoggerFactory.getLogger(VSerializable::class.java)

suspend fun startLibrary(vertx: Vertx, kodein: DirectDI) {
  vertx.eventBus().registerCodec(PassThroughCodec())

  log.debug("Deploying VerticleFactoryVerticle")

  val startupVerticlesLists =
    listOf (
      listOf(
        RouterVerticle::class,
        EventSourcingApiVerticle::class
      )
    )

  startupVerticlesLists.forEach { verticlesToDeploy: List<KClass<out CoroutineVerticle>> ->
    log.debugIf { "Instantiating verticles: $verticlesToDeploy" }
    verticlesToDeploy.forEach { classToDeploy ->
      log.debugIf { "Instantiating verticle: ${classToDeploy.qualifiedName}" }
      // TODO: what if this returns a failure, need to test the handling verticle to see what
      //  happens with a failure
      kodein.directDI.Instance(TT(classToDeploy))
    }
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////

data class CommandsAndEventsSystem(
  val commandSender: CommandSender,
  val commandRegistrar: CommandRegistrar,
  val eventPublisher: EventPublisher,
  val eventRegistrar: EventRegistrar,
)

suspend fun createCommandsAndEventsSystem(eventBus: EventBus): CommandsAndEventsSystem {
  val commandSender = CommandSender(eventBus)
  val commandRegistrar = CommandRegistrar(eventBus, commandSender)

  val eventPublisher = EventPublisher(eventBus)
  val eventRegistrar = EventRegistrar(eventBus, commandSender)

  return CommandsAndEventsSystem(commandSender, commandRegistrar, eventPublisher, eventRegistrar)
}
