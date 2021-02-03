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
import com.snapleft.test.utilities.runUpdateSql
import com.snapleft.vertx.AggregateEvent
import com.snapleft.vertx.AggregateId
import com.snapleft.vertx.AggregateSnapshot
import com.snapleft.vertx.AggregateVersion
import com.snapleft.vertx.commands.CommandFailedDueToException
import com.snapleft.vertx.setupVertxKodein
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.vertx.config.ConfigRetriever
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.extension.ExtendWith
import org.kodein.di.instance
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

data class TestSnapshot(override val aggregateId: AggregateId,
  override val aggregateVersion: AggregateVersion, val testData: String): AggregateSnapshot

@ExtendWith(VertxExtension::class)
class PostgresIntegrationTests {
  val vertx = Vertx.vertx()
  private val log = LoggerFactory.getLogger(PostgresIntegrationTests::class.java)

  val verticlesToDeploy = listOf<KClass<out CoroutineVerticle>>(EventSourcingApiVerticle::class)

  @Test()
  @Timeout(value = 3, timeUnit = TimeUnit.SECONDS)
  @EnabledIfSystemProperty(named = "integration-tests", matches = "true")
  fun snapshotInsertAndQuery() = runBlocking<Unit> {
    var testContext: VertxTestContext = VertxTestContext()

    log.debug("Starting test...")

    val (_, kodein) = setupVertxKodein(listOf(), vertx)
    val retriever = ConfigRetriever.create(vertx)
    val configuration: JsonObject = awaitResult { retriever.getConfig(it) }
    val deploymentOptions = DeploymentOptions()
    deploymentOptions.config = configuration

    val verticle = kodein.instance<EventSourcingApiVerticle>()

    val runtimeInMilliseconds = measureTimeMillis {
      val aggregateId = AggregateId(UUID.randomUUID().toString())
      val snapshot = TestSnapshot(aggregateId, AggregateVersion(1), "This is test data")

      val addResult = verticle.storeAggregateSnapshot(snapshot)

      log.debug(addResult.toString())
      (addResult is Either.Right<*>) shouldBe true

      val loadResult: Either<CommandFailedDueToException, AggregateSnapshot?> =
        verticle.loadLatestAggregateSnapshot(LatestAggregateSnapshotQuery(aggregateId))

      testContext.verify {
        when (loadResult) {
          is Either.Left -> fail("load result was a failure: ${loadResult.a}")
          is Either.Right -> {
            val response: AggregateSnapshot? = loadResult.b

            when (loadResult.b) {
              null -> { fail("No snapshot was returned") }
              else -> {
                response shouldBe snapshot
              }
            }
          }
        }
      }

      runUpdateSql(
        "delete from event_sourcing.snapshots where aggregate_id = $1 and version_number = $2",
        Tuple.of(aggregateId.id, 1), vertx)
    }

    println("Total runtime without initialization $runtimeInMilliseconds")
  }

  @Test(timeout = 5000)
  fun eventsInsertAndQuery() = runBlocking<Unit> {
    val context = VertxTestContext()
    val (_, kodein) = setupVertxKodein(listOf(), vertx)
    val retriever = ConfigRetriever.create(vertx)
    val configuration: JsonObject = awaitResult { h -> retriever.getConfig(h) }
    val deploymentOptions = DeploymentOptions()
    deploymentOptions.config = configuration

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
          fail(storeResult.a.toString())
        }
        is Either.Right -> {
          val loadResult =
            verticle.loadAggregateEvents(AggregateEventsQuery(aggregateId, Long.MIN_VALUE))

          when (loadResult) {
            is Either.Left -> {
              println(loadResult.a.toString())
              fail(loadResult.a.toString())
            }
            is Either.Right -> {
              val loadedEvents = loadResult.b
              if (loadedEvents.isEmpty()) {
                fail("No events were returned")
              }

              loadedEvents shouldBe events
            }
          }
        }
      }
    }

    println("Total events runtime without initialization $runtimeInMilliseconds")

    context.completeNow()
  }
}
