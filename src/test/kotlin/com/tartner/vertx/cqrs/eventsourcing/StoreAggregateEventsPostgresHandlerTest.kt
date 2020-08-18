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

package com.tartner.vertx.cqrs.eventsourcing

class StoreAggregateEventsPostgresHandlerTest {
//  val databasePool: EventSourcingPool = mockk()
//  val connection: SqlConnection = mockk(relaxed = true)
//
//  val log: Logger = mockk(relaxed = true)
//
//  val databaseMapper = TypedObjectMapper.default
//
//  val storeAggregateSnapshotPostgresHandler = EventSourcingApi(databasePool, databaseMapper, log)
//
//  val aggregateId = AggregateId(UUID.randomUUID().toString())
//  val aggregateVersion = AggregateVersion(1)
//
//  val testSnapshot = TestSnapshot(aggregateId, aggregateVersion, "This is test data")
//  val testSnapshotJson = databaseMapper.writeValueAsString(testSnapshot)
//
//  val expectedTuple = Tuple.of(aggregateId.id, aggregateVersion.version, testSnapshotJson)
//  val expectedPreparedQuery: PreparedQuery<RowSet<Row>> = mockk()
//  val sqlResult: RowSet<Row> = mockk()
//
//  lateinit var preparedQueryCaptures: PreparedQueryCaptures
//
//  @Test
//  fun storeAggregateEvents() {
//    runBlocking {
//      every { log.isDebugEnabled } returns true
//      setupSuccessfulGetConnection(databasePool, connection)
//      preparedQueryCaptures = setupSuccessfulPreparedQuery(connection, sqlResult)
//
//      every { sqlResult.rowCount() } returns 1
//
//      val reply = storeAggregateSnapshotPostgresHandler.storeAggregateSnapshotActAndReply(testSnapshot)
//
//      reply shouldBe successReplyRight
//
//      commonStoreSnapshotPreparedQueryVerify()
//    }
//  }
//
//  @Test
//  fun storeAggregateSnapshotConnectionFail() {
//    runBlocking {
//
//      commonStoreSnapshotSetup()
//
//      val expectedException = RuntimeException("Expected")
//      setupFailedGetConnection(databasePool, expectedException)
//
//      val reply = storeAggregateSnapshotPostgresHandler.storeAggregateSnapshotActAndReply(testSnapshot)
//
//      reply shouldBe CommandFailedDueToException(expectedException).left()
//
//      commonStoreSnapshotVerify()
//    }
//  }
//
//  @Test
//  fun storeAggregateSnapshotNoRecordsUpdated() {
//    runBlocking {
//      commonStoreSnapshotPreparedQuerySetup()
//
//      every { sqlResult.rowCount() } returns 0
//
//      val reply = storeAggregateSnapshotPostgresHandler.storeAggregateSnapshotActAndReply(testSnapshot)
//
//      if (reply is Either.Left) {
//        val error = reply.a as ErrorReply
//        error.message shouldContain("Unable to store aggregate snapshot for snapshot")
//      } else {
//        fail("Reply is wrong type, should be ErrorReply: $reply")
//      }
//
//      commonStoreSnapshotPreparedQueryVerify()
//    }
//  }
//
//  private fun commonStoreSnapshotSetup() {
//    every { log.isDebugEnabled } returns true
//  }
//
//  private fun commonStoreSnapshotPreparedQuerySetup() {
//    commonStoreSnapshotSetup()
//
//    setupSuccessfulGetConnection(databasePool, connection)
//
//    preparedQueryCaptures = setupSuccessfulPreparedQuery(connection, sqlResult)
//  }
//
//  private fun commonStoreSnapshotVerify() {
//    verifyAll {
//      databaseMapper.writeValueAsString(testSnapshot)
//      databasePool.getConnection()
//    }
//  }
//
//  private fun commonStoreSnapshotPreparedQueryVerify() {
//    commonStoreSnapshotVerify()
//    verifyAll {
////      preparedQueryCaptures.tupleSlot.captured shouldContain expectedTuple
//      preparedQueryCaptures.sqlSlot.captured shouldContain "insert into"
//      preparedQueryCaptures.sqlSlot.captured shouldContain "snapshots"
//      connection.toString()
//      connection.preparedQuery(any())
//      sqlResult.rowCount()
//      connection.close()
//    }
//  }
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

