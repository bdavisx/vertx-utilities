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

package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.Either
import arrow.core.left
import com.tartner.vertx.AggregateId
import com.tartner.vertx.AggregateSnapshot
import com.tartner.vertx.AggregateVersion
import com.tartner.vertx.ErrorReply
import com.tartner.vertx.FailureReply
import com.tartner.vertx.Reply
import com.tartner.vertx.SuccessReply
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.commands.CommandFailedDueToException
import com.tartner.vertx.cqrs.database.EventSourcingPool
import com.tartner.vertx.successReplyRight
import io.kotlintest.fail
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyAll
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.logging.Logger
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.SqlResult
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.impl.command.CommandResponse
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.UUID
import java.util.stream.Collector

data class TestSnapshot(override val aggregateId: AggregateId,
  override val aggregateVersion: AggregateVersion, val testData: String): AggregateSnapshot

class EventSourcedAggregateDataAccessTest() {
  val databasePool: EventSourcingPool = mockk()
  val databaseMapper: TypedObjectMapper = mockk()
  val reply: Reply = mockk()
  val connection: SqlConnection = mockk()

  val log: Logger = mockk()

  val verticle: EventSourcedAggregateDataAccess =
    EventSourcedAggregateDataAccess(databasePool, databaseMapper, log)

  val aggregateId = AggregateId(UUID.randomUUID().toString())
  val aggregateVersion = AggregateVersion(1)

  val testSnapshot = TestSnapshot(aggregateId, aggregateVersion, "This is test data")

  @Test
  fun storeAggregateSnapshot() {
    runBlocking {

      val json = """{ "key": "value" }"""
      val expectedTuple = Tuple.of(aggregateId.id, aggregateVersion.version, json)
      val sqlResult: SqlResult<List<Row>> = mockk()

      every { databaseMapper.writeValueAsString(testSnapshot) } returns json
      every { log.isDebugEnabled } returns true
      every { log.debug(any()) } returns Unit
      every { connection.toString() } returns "Hello"

      val getConnectionSlot = slot<Handler<AsyncResult<SqlConnection>>>()
      coEvery { databasePool.getConnection(capture(getConnectionSlot)) } answers {
        getConnectionSlot.captured.handle(CommandResponse.success(connection))
      }

      val preparedQuerySlot = slot<Handler<AsyncResult<SqlResult<List<Row>>>>>()
      coEvery { connection.preparedQuery(any(), expectedTuple, any<Collector<Row,*,List<Row>>>(),
        capture(preparedQuerySlot)) } answers {
        preparedQuerySlot.captured.handle(CommandResponse.success(sqlResult))
        connection
      }

      every { sqlResult.rowCount() } returns 1
      val replySlot = slot<Any>()
      every { reply(capture(replySlot)) } answers {Unit}
      every { connection.close() } answers {Unit}

      verticle.storeAggregateSnapshot(StoreAggregateSnapshotCommand(aggregateId, testSnapshot), reply)

      replySlot.captured shouldBe successReplyRight

      verifyAll {
        databaseMapper.writeValueAsString(testSnapshot)
        databasePool.getConnection(any())
        connection.toString()
        connection.preparedQuery(any(), expectedTuple, any<Collector<Row,*,List<Row>>>(), any())
        sqlResult.rowCount()
        connection.close()
      }
    }
  }

  @Test
  fun storeAggregateSnapshotConnectionFail() {
    runBlocking {

      val json = """{ "key": "value" }"""
      val expectedTuple = Tuple.of(aggregateId.id, aggregateVersion.version, json)
      val sqlResult: SqlResult<List<Row>> = mockk()

      every { databaseMapper.writeValueAsString(testSnapshot) } returns json
      every { log.isDebugEnabled } returns true

      val getConnectionSlot = slot<Handler<AsyncResult<SqlConnection>>>()
      val expectedException = RuntimeException("Expected")
      coEvery { databasePool.getConnection(capture(getConnectionSlot)) } answers {
        getConnectionSlot.captured.handle(CommandResponse.failure(expectedException))
      }

      every { log.debug(any()) } returns Unit
      every { log.warn(any(), expectedException) } returns Unit

      val replySlot = slot<Any>()
      every { reply(capture(replySlot)) } answers {Unit}

      verticle.storeAggregateSnapshot(StoreAggregateSnapshotCommand(aggregateId, testSnapshot), reply)

      replySlot.captured shouldBe CommandFailedDueToException(expectedException).left()

      verifyAll {
        databaseMapper.writeValueAsString(testSnapshot)
        databasePool.getConnection(any())
      }}
  }

  @Test
  fun storeAggregateSnapshotNoRecordsUpdated() {
    runBlocking {

      val json = """{ "key": "value" }"""
      val expectedTuple = Tuple.of(aggregateId.id, aggregateVersion.version, json)
      val sqlResult: SqlResult<List<Row>> = mockk()

      every { databaseMapper.writeValueAsString(testSnapshot) } returns json
      every { log.isDebugEnabled } returns true
      every { log.debug(any()) } returns Unit
      every { connection.toString() } returns "Hello"

      val getConnectionSlot = slot<Handler<AsyncResult<SqlConnection>>>()
      coEvery { databasePool.getConnection(capture(getConnectionSlot)) } answers {
        getConnectionSlot.captured.handle(CommandResponse.success(connection))
      }

      val preparedQuerySlot = slot<Handler<AsyncResult<SqlResult<List<Row>>>>>()
      coEvery { connection.preparedQuery(any(), expectedTuple, any<Collector<Row,*,List<Row>>>(),
        capture(preparedQuerySlot)) } answers {
        preparedQuerySlot.captured.handle(CommandResponse.success(sqlResult))
        connection
      }

      every { sqlResult.rowCount() } returns 0
      val replySlot = slot<Either<FailureReply, SuccessReply>>()
      every { reply(capture(replySlot)) } answers {Unit}
      every { connection.close() } answers {Unit}

      verticle.storeAggregateSnapshot(StoreAggregateSnapshotCommand(aggregateId, testSnapshot), reply)

      val capturedReply = replySlot.captured

      if (capturedReply is Either.Left<*>) {
        val error = capturedReply.a as ErrorReply
        error.message shouldContain("Unable to store aggregate snapshot for snapshot")
      } else {
        fail("Reply is wrong type, should be ErrorReply: $capturedReply")
      }

      verifyAll {
        databaseMapper.writeValueAsString(testSnapshot)
        databasePool.getConnection(any())
        connection.toString()
        connection.preparedQuery(any(), expectedTuple, any<Collector<Row,*,List<Row>>>(), any())
        sqlResult.rowCount()
        connection.close()
      }
    }
  }
}
//          val loadResult = commandSender.sendA<FailureReply, SuccessReply>(
//            LoadLatestAggregateSnapshotCommand(aggregateId))
//
//          when (loadResult) {
//            is Either.Left<*> -> context.fail()
//            is Either.Right<*> -> {
//              if (loadResult.b == null) { context.fail("No snapshot was returned") }
//
//              val response = loadResult.b as LoadLatestAggregateSnapshotResponse
//              val loadedSnapshot: AggregateSnapshot = response.possibleSnapshot.getOrElse {
//                fail("No snapshot found") }
//              context.assertEquals(snapshot, loadedSnapshot)
//            }
//          }
//
//          val databaseUtils = DatabaseTestUtilities()
//          databaseUtils.runUpdateSql(
//            "delete from event_sourcing.snapshots where aggregate_id = $1 and version_number = $2",
//            Tuple.of(aggregateId.id, 1), vertx)
//        }
//
//        println("Total runtime without initialization $runtimeInMilliseconds")
//        async.complete()
//      } catch(ex: Throwable) {
//        context.fail(ex)
//      }
//    }}
//  }
//
//  @Test(timeout = 5000)
//  fun eventsInsertAndQuery(context: TestContext) {
//    val async = context.async()
//
//    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
//      try {
//        val (_, injector) = setupVertxKodein(listOf(), vertx, context)
//        val retriever = ConfigRetriever.create(vertx)
//        val configuration: JsonObject = awaitResult { h -> retriever.getConfig(h) }
//        val deploymentOptions = DeploymentOptions()
//        deploymentOptions.config = configuration
//
//        val deployer: VerticleDeployer = injector.i()
//        val verticle: EventSourcedAggregateDataAccess = injector.dkodein.instance()
//        awaitResult<String> { vertx.deployVerticle(verticle, deploymentOptions, it) }
//
//        val commandSender: CommandSender = injector.i()
//        val commandRegistrar: CommandRegistrar = injector.i()
//
//        val runtimeInMilliseconds = measureTimeMillis {
//          val aggregateId = AggregateId(UUID.randomUUID().toString())
//
//          val events = mutableListOf<AggregateEvent>()
//          var aggregateVersion: Long = 0
//          for (i in 1..10) {
//            events.add(EventSourcedTestAggregateCreated(aggregateId, AggregateVersion(aggregateVersion++), "Name"))
//            events.add(EventSourcedTestAggregateNameChanged(aggregateId, AggregateVersion(aggregateVersion++), "New Name"))
//          }
//
//          val storeResult = commandSender.sendA<FailureReply, SuccessReply>(
//            StoreAggregateEventsCommand(aggregateId, events))
//
//          when (storeResult) {
//            is Either.Left -> {
//              println(storeResult.a.toString())
//              context.fail(storeResult.a.toString())
//            }
//            is Either.Right -> {
//              val loadResult = commandSender.sendA<FailureReply, SuccessReply>(
//                LoadAggregateEventsCommand(aggregateId, Long.MIN_VALUE))
//
//              when (loadResult) {
//                is Either.Left -> {
//                  println(loadResult.a.toString())
//                  context.fail(loadResult.a.toString())
//                }
//                is Either.Right -> {
//                  val loadedEvents = loadResult.b as LoadAggregateEventsResponse
//                  if (loadedEvents.events.isEmpty()) {
//                    context.fail("No events were returned")
//                  }
//
//                  loadedEvents.events shouldBe events
//                }
//              }
//            }
//          }
//        }
//
//        println("Total events runtime without initialization $runtimeInMilliseconds")
//
//        async.complete()
//      } catch(ex: Throwable) {
//        context.fail(ex)
//      }
//    }}
//  }
//}

