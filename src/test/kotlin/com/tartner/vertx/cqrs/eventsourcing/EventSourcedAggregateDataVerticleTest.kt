/*
 * Copyright (c) 2019 the original author or authors.
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

package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.Either
import arrow.core.getOrElse
import com.tartner.test.utilities.AbstractVertxTest
import com.tartner.test.utilities.DatabaseTestUtilities
import com.tartner.test.utilities.TestConfigurationDefaults
import com.tartner.vertx.AggregateId
import com.tartner.vertx.AggregateSnapshot
import com.tartner.vertx.FailureReply
import com.tartner.vertx.SuccessReply
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.kodein.VerticleDeployer
import com.tartner.vertx.kodein.i
import com.tartner.vertx.setupVertxKodein
import io.kotlintest.fail
import io.vertx.config.ConfigRetriever
import io.vertx.core.DeploymentOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.json.JsonArray
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.generic.instance
import java.util.UUID
import kotlin.system.measureTimeMillis

@RunWith(VertxUnitRunner::class)
class EventSourcedAggregateDataVerticleTest: AbstractVertxTest() {
  @Test(timeout = 2500)
  fun snapshotInsertAndQuery(context: io.vertx.ext.unit.TestContext) {
    val async = context.async()

    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
      try {
        val (_, injector) = setupVertxKodein(listOf(), vertx, context)
        val retriever = ConfigRetriever.create(vertx)
        val configuration: JsonObject = awaitResult { h -> retriever.getConfig(h) }
        val deploymentOptions = DeploymentOptions()
        deploymentOptions.config = configuration

        val deployer: VerticleDeployer = injector.i()
        val verticle: EventSourcedAggregateDataVerticle = injector.dkodein.instance()
        awaitResult<String> { vertx.deployVerticle(verticle, deploymentOptions, it) }

        val commandSender: CommandSender = injector.i()
        val commandRegistrar: CommandRegistrar = injector.i()

        val runtimeInMilliseconds = measureTimeMillis {
          val aggregateId = AggregateId(UUID.randomUUID().toString())
          val snapshot = TestSnapshot(aggregateId, 1, "This is test data")

          val eventBus: EventBus = injector.i()
          val addResult = commandSender.sendA<FailureReply, SuccessReply>(
            StoreAggregateSnapshotCommand(aggregateId, snapshot))

          context.assertTrue(addResult is Either.Right<*>)

          val loadResult = commandSender.sendA<FailureReply, SuccessReply>(
            LoadLatestAggregateSnapshotCommand(aggregateId))

          when (loadResult) {
            is Either.Left<*> -> context.fail()
            is Either.Right<*> -> {
              val response = loadResult.b as LoadLatestAggregateSnapshotResponse
              if (response == null) { context.fail("No snapshot was returned") }
              else {
                val loadedSnapshot: AggregateSnapshot = response.possibleSnapshot.getOrElse {
                  fail("No snapshot found") }
                context.assertEquals(snapshot, loadedSnapshot)
              }
            }
          }

          val databaseUtils: DatabaseTestUtilities = injector.i()
          databaseUtils.runUpdateSql(
            "delete from event_sourcing.snapshots where aggregate_id = ? and version_number = ?",
            JsonArray(aggregateId, 1), vertx, configuration)
        }

        println("Total runtime without initialization $runtimeInMilliseconds")
        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }

//  @Test(timeout = 5000)
//  fun eventsInsertAndQuery(context: TestContext) {
//    val async = context.async()
//
//    vertx.runOnContext { launch(vertx.dispatcher()) {
//      try {
//        val injector = setupVertxKodein(listOf(), vertx, context)
//        val configuration: JsonObject = TestConfigurationDefaults.buildConfiguration(vertx)
//        val deploymentOptions = DeploymentOptions()
//        deploymentOptions.config = configuration
//
//        val deployer: VerticleDeployer = injector.i()
//        val provider = injector.direct.provider<EventSourcedAggregateDataVerticle>()
//        val verticle: EventSourcedAggregateDataVerticle = provider()
//        awaitResult<String> { vertx.deployVerticle(verticle, deploymentOptions, it) }
//
//        val commandSender: CommandSender = injector.i()
//        val commandRegistrar: CommandRegistrar = injector.i()
//
//        val runtimeInMilliseconds = measureTimeMillis {
//          val aggregateId = UUID.randomUUID()
//
//          val events = mutableListOf<AggregateEvent>()
//          var aggregateVersion: Long = 0
//          for (i in 1..1000) {
//            events.add(EventSourcedTestAggregateCreated(aggregateId, aggregateVersion++, "Name"))
//            events.add(EventSourcedTestAggregateNameChanged(aggregateId, aggregateVersion++, "New Name"))
//          }
//
//          val addResult = verticle.storeAggregateEvents(aggregateId, events)
//          context.assertTrue(addResult is Either.Right)
//
//          val loadResult =
//            verticle.loadAggregateEvents(aggregateId, EventSourcingDelegate.initialVersion)
//
//          when (loadResult) {
//            is Either.Left -> context.fail(loadResult.a.toString())
//            is Either.Right -> {
//              val loadedEvents: List<AggregateEvent> = loadResult.b
//              if (loadedEvents.isEmpty()) {
//                context.fail("No events were returned")
//              }
//
//              loadedEvents shouldBe events
//            }
//          }
//        }
//
//        println("Total events runtime without initialization $runtimeInMilliseconds")
//        async.complete()
//      } catch(ex: Throwable) {
//        context.fail(ex)
//      }
//    }}
//  }
}

data class TestSnapshot(override val aggregateId: AggregateId, override val aggregateVersion: Long,
  val testData: String): AggregateSnapshot