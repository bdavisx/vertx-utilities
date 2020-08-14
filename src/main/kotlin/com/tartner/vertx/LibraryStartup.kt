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
import com.tartner.vertx.codecs.EventBusJacksonJsonCodec
import com.tartner.vertx.cqrs.eventsourcing.AggregateEventsQueryHandler
import com.tartner.vertx.cqrs.eventsourcing.LatestAggregateSnapshotQueryHandler
import com.tartner.vertx.cqrs.eventsourcing.StoreAggregateEventsPostgresHandler
import com.tartner.vertx.cqrs.eventsourcing.StoreAggregateSnapshotPostgresHandler
import com.tartner.vertx.kodein.KodeinVerticleFactoryVerticle
import com.tartner.vertx.kodein.VerticleDeployer
import com.tartner.vertx.kodein.i
import io.vertx.core.CompositeFuture
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.kodein.di.DKodein
import org.kodein.di.generic.instance
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

private val log = LoggerFactory.getLogger(VSerializable::class.java)

suspend fun startLibrary(vertx: Vertx, kodein: DKodein) {
  log.debug("Registering the EventBusJacksonJsonCodec codec")
  vertx.eventBus().registerCodec(EventBusJacksonJsonCodec(kodein.i()))

  log.debug("Deploying VerticleFactoryVerticle")
  val factoryVerticle = kodein.instance<KodeinVerticleFactoryVerticle>()
  val verticleDeployer = kodein.instance<VerticleDeployer>()
  CompositeFuture.all(
    verticleDeployer.deployVerticles(vertx, listOf(factoryVerticle)).map{it.future()}).await()
  log.debug("VerticleFactoryVerticle deployed")

  val startupVerticlesLists =
    listOf (
      listOf(
        RouterVerticle::class
      )
    )

  startupVerticlesLists.forEach { verticlesToDeploy: List<KClass<out CoroutineVerticle>> ->
    log.debugIf { "Instantiating verticles: $verticlesToDeploy" }
    verticlesToDeploy.forEach { classToDeploy ->
      log.debugIf { "Instantiating verticle: ${classToDeploy.qualifiedName}" }
      // TODO: what if this returns a failure, need to test the handling verticle to see what
      //  happens with a failure
      factoryVerticle.deployVerticleInstances(classToDeploy)
    }
  }

  val startupDelegates: List<KClass<out CoroutineDelegate>> = listOf(
    StoreAggregateEventsPostgresHandler::class,
    StoreAggregateSnapshotPostgresHandler::class,
    AggregateEventsQueryHandler::class,
    LatestAggregateSnapshotQueryHandler::class)

  startupDelegates.forEach { classToDeploy ->
    log.debugIf { "Instantiating verticle: ${classToDeploy.qualifiedName}" }
    // TODO: what if this returns a failure, need to test the handling verticle to see what
    //  happens with a failure
    factoryVerticle.deployVerticleDelegates(classToDeploy)
  }
}
