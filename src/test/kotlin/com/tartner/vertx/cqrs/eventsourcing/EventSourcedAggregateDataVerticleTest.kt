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
import com.tartner.vertx.AggregateEvent
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
import io.kotlintest.shouldBe
import io.vertx.config.ConfigRetriever
import io.vertx.core.DeploymentOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.generic.instance
import java.util.UUID
import kotlin.system.measureTimeMillis

@RunWith(VertxUnitRunner::class)
class EventSourcedAggregateDataVerticleTest: AbstractVertxTest() {
  private val log = LoggerFactory.getLogger(EventSourcedAggregateDataVerticleTest::class.java)

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

          log.debug(addResult.toString())
          context.assertTrue(addResult is Either.Right<*>)

          val loadResult = commandSender.sendA<FailureReply, SuccessReply>(
            LoadLatestAggregateSnapshotCommand(aggregateId))

          when (loadResult) {
            is Either.Left<*> -> context.fail()
            is Either.Right<*> -> {
              if (loadResult.b == null) { context.fail("No snapshot was returned") }

              val response = loadResult.b as LoadLatestAggregateSnapshotResponse
              val loadedSnapshot: AggregateSnapshot = response.possibleSnapshot.getOrElse {
                fail("No snapshot found") }
              context.assertEquals(snapshot, loadedSnapshot)
            }
          }

          val databaseUtils = DatabaseTestUtilities()
          databaseUtils.runUpdateSql(
            "delete from event_sourcing.snapshots where aggregate_id = $1 and version_number = $2",
            Tuple.of(aggregateId.id, 1), vertx)
        }

        println("Total runtime without initialization $runtimeInMilliseconds")
        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }

  @Test(timeout = 5000)
  fun eventsInsertAndQuery(context: TestContext) {
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

          val events = mutableListOf<AggregateEvent>()
          var aggregateVersion: Long = 0
          for (i in 1..1000) {
            events.add(EventSourcedTestAggregateCreated(aggregateId, aggregateVersion++, "Name"))
            events.add(EventSourcedTestAggregateNameChanged(aggregateId, aggregateVersion++, "New Name"))
          }

          val storeResult = commandSender.sendA<FailureReply, SuccessReply>(
            StoreAggregateEventsCommand(aggregateId, events))

          context.assertTrue(storeResult is Either.Right)

          val loadResult = commandSender.sendA<FailureReply, SuccessReply>(
            LoadAggregateEventsCommand(aggregateId, Long.MIN_VALUE))

          when (loadResult) {
            is Either.Left -> context.fail(loadResult.a.toString())
            is Either.Right -> {
              val loadedEvents = loadResult.b as LoadAggregateEventsResponse
              if (loadedEvents.events.isEmpty()) {
                context.fail("No events were returned")
              }

              loadedEvents.events shouldBe events
            }
          }
        }

        println("Total events runtime without initialization $runtimeInMilliseconds")

        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }
}

data class TestSnapshot(override val aggregateId: AggregateId, override val aggregateVersion: Long,
  val testData: String): AggregateSnapshot
