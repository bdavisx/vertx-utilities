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
import com.snapleft.vertx.codecs.EventBusJacksonJsonCodec
import com.snapleft.vertx.codecs.PassThroughCodec
import com.snapleft.vertx.cqrs.eventsourcing.EventSourcingApiVerticle
import com.snapleft.vertx.dependencyinjection.i
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.kodein.di.DirectDI
import org.kodein.di.TT
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

private val log = LoggerFactory.getLogger(VSerializable::class.java)

suspend fun startLibrary(vertx: Vertx, kodein: DirectDI) {
  log.debug("Registering the EventBusJacksonJsonCodec codec")
  vertx.eventBus().registerCodec(EventBusJacksonJsonCodec(kodein.i()))

  val codecName = CodeMessage::class.qualifiedName!!
  log.debug("Registering the $codecName codec")
  vertx.eventBus().registerCodec(
    PassThroughCodec<CodeMessage<*, DirectCallVerticle<*>>>(codecName))

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
