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

package com.snapleft.vertx.cqrs.eventsourcing

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import com.snapleft.test.utilities.AbstractVertxTest
import com.snapleft.test.utilities.runUpdateSql
import com.snapleft.utilities.debugIf
import com.snapleft.vertx.AggregateEvent
import com.snapleft.vertx.AggregateId
import com.snapleft.vertx.AggregateSnapshot
import com.snapleft.vertx.AggregateVersion
import com.snapleft.vertx.CodeMessage
import com.snapleft.vertx.DirectCallVerticle
import com.snapleft.vertx.codecs.PassThroughCodec
import com.snapleft.vertx.commands.CommandFailedDueToException
import com.snapleft.vertx.dependencyinjection.VerticleDeployer
import com.snapleft.vertx.setupVertxKodein
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.vertx.config.ConfigRetriever
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.TT
import org.kodein.di.instance
import org.kodein.type.generic
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

data class TestSnapshot(override val aggregateId: AggregateId,
  override val aggregateVersion: AggregateVersion, val testData: String): AggregateSnapshot

@RunWith(VertxUnitRunner::class)
class PostgresIntegrationTests: AbstractVertxTest() {
  private val log = LoggerFactory.getLogger(PostgresIntegrationTests::class.java)

  val verticlesToDeploy = listOf<KClass<out CoroutineVerticle>>(EventSourcingApiVerticle::class)

  @Test(timeout = 2500)
  @Ignore
  fun snapshotInsertAndQuery(context: TestContext) {
    val async = context.async()

    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
      try {
        val (_, kodein) = setupVertxKodein(listOf(), vertx, context)
        val retriever = ConfigRetriever.create(vertx)
        val configuration: JsonObject = awaitResult { retriever.getConfig(it) }
        val deploymentOptions = DeploymentOptions()
        deploymentOptions.config = configuration

        vertx.eventBus().registerCodec(PassThroughCodec<CodeMessage<*, DirectCallVerticle<*>>>(
          CodeMessage::class.qualifiedName!!))

        val verticle = kodein.instance<EventSourcingApiVerticle>()

        val runtimeInMilliseconds = measureTimeMillis {
          val aggregateId = AggregateId(UUID.randomUUID().toString())
          val snapshot = TestSnapshot(aggregateId, AggregateVersion(1), "This is test data")

          val addResult = verticle.storeAggregateSnapshot(snapshot)

          log.debug(addResult.toString())
          context.assertTrue(addResult is Either.Right<*>)

          val loadResult: Either<CommandFailedDueToException, Option<AggregateSnapshot>> =
            verticle.loadLatestAggregateSnapshot(LatestAggregateSnapshotQuery(aggregateId))

          when (loadResult) {
            is Either.Left -> context.fail()
            is Either.Right -> {
              if (loadResult.b.isEmpty()) { context.fail("No snapshot was returned") }

              val response: Option<AggregateSnapshot> = loadResult.b
              val loadedSnapshot: AggregateSnapshot = response.getOrElse {
                fail("No snapshot found") }
              context.assertEquals(snapshot, loadedSnapshot)
            }
          }

          runUpdateSql(
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
  @Ignore
  fun eventsInsertAndQuery(context: TestContext) {
    val async = context.async()

    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
      try {
        val (_, kodein) = setupVertxKodein(listOf(), vertx, context)
        val retriever = ConfigRetriever.create(vertx)
        val configuration: JsonObject = awaitResult { h -> retriever.getConfig(h) }
        val deploymentOptions = DeploymentOptions()
        deploymentOptions.config = configuration

        vertx.eventBus().registerCodec(
          PassThroughCodec<CodeMessage<*, DirectCallVerticle<*>>>(CodeMessage::class.qualifiedName!!))

        vertx.eventBus().registerDefaultCodec(
          Any::class.java, PassThroughCodec<Any>(Any::class.qualifiedName!!))

        val verticle = kodein.instance<EventSourcingApiVerticle>()

        val runtimeInMilliseconds = measureTimeMillis {
          val aggregateId = AggregateId(UUID.randomUUID().toString())

          val events = mutableListOf<AggregateEvent>()
          var aggregateVersion: Long = 0
          for (i in 1..10) {
            events.add(
              EventSourcedTestAggregateCreated(aggregateId, AggregateVersion(aggregateVersion++), "Name"))
            events.add(EventSourcedTestAggregateNameChanged(aggregateId, AggregateVersion(aggregateVersion++), "New Name"))
          }

          val storeResult =
            verticle.storeAggregateEvents(StoreAggregateEventsCommand(aggregateId, events))

          when (storeResult) {
            is Either.Left -> {
              println(storeResult.a.toString())
              context.fail(storeResult.a.toString())
            }
            is Either.Right -> {
              val loadResult =
                verticle.loadAggregateEvents(AggregateEventsQuery(aggregateId, Long.MIN_VALUE))

              when (loadResult) {
                is Either.Left -> {
                  println(loadResult.a.toString())
                  context.fail(loadResult.a.toString())
                }
                is Either.Right -> {
                  val loadedEvents = loadResult.b
                  if (loadedEvents.isEmpty()) {
                    context.fail("No events were returned")
                  }

                  loadedEvents shouldBe events
                }
              }
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
