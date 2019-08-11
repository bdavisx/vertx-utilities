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

import arrow.core.Option
import arrow.core.left
import arrow.core.toOption
import com.fasterxml.jackson.module.kotlin.readValue
import com.tartner.vertx.AggregateEvent
import com.tartner.vertx.AggregateId
import com.tartner.vertx.AggregateSnapshot
import com.tartner.vertx.CommandHandlingCoroutineDelegate
import com.tartner.vertx.ErrorReply
import com.tartner.vertx.Reply
import com.tartner.vertx.SuccessReply
import com.tartner.vertx.SuspendableReplyMessageHandler
import com.tartner.vertx.VCommand
import com.tartner.vertx.batchWithParamsA
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.commands.CommandFailedDueToException
import com.tartner.vertx.cqrs.database.EventSourcingPool
import com.tartner.vertx.debugIf
import com.tartner.vertx.functional.toLeft
import com.tartner.vertx.functional.toRight
import com.tartner.vertx.getConnectionA
import com.tartner.vertx.queryWithParamsA
import com.tartner.vertx.successReplyRight
import com.tartner.vertx.updateWithParamsA
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass

data class LoadAggregateEventsCommand(val aggregateId: AggregateId, val aggregateVersion: Long): VCommand
data class LoadAggregateEventsResponse(val aggregateId: AggregateId, val aggregateVersion: Long,
  val events: List<AggregateEvent>): SuccessReply

data class StoreAggregateEventsCommand(val aggregateId: AggregateId, val events: List<AggregateEvent>): VCommand
data class StoreAggregateSnapshotCommand(val aggregateId: AggregateId, val snapshot: AggregateSnapshot): VCommand

data class LoadLatestAggregateSnapshotCommand(val aggregateId: AggregateId): VCommand
data class LoadLatestAggregateSnapshotResponse(val aggregateId: AggregateId,
  val possibleSnapshot: Option<AggregateSnapshot>): SuccessReply

class EventSourcedAggregateDataAccess(
  private val databasePool: EventSourcingPool,
  private val databaseMapper: TypedObjectMapper,
  private val log: Logger = LoggerFactory.getLogger(EventSourcedAggregateDataAccess::class.java)
): CommandHandlingCoroutineDelegate {

  override val commandWithReplyMessageHandlers: Map<KClass<*>, SuspendableReplyMessageHandler<*>> = mapOf(
      LoadAggregateEventsCommand::class to ::loadAggregateEvents,
      LoadLatestAggregateSnapshotCommand::class to ::loadLatestAggregateSnapshot,
      StoreAggregateEventsCommand::class to ::storeAggregateEvents,
      StoreAggregateSnapshotCommand::class to ::storeAggregateSnapshot)

  companion object {
    @Language("PostgreSQL")
    private val selectSnapshotSql = """
      select data
      from event_sourcing.snapshots
      where aggregate_id = $1
      order by version_number desc
      limit 1""".trimIndent()

    @Language("PostgreSQL")
    private val selectEventsSql = """
      select data
      from event_sourcing.events
      where aggregate_id = $1 and version_number >= $2
      order by version_number
      """.trimIndent()

    @Language("PostgreSQL")
    private val insertEventsSql = """
      insert into event_sourcing.events (aggregate_id, version_number, data)
      values ($1, $2, cast($3 as json))""".trimIndent()

    @Language("PostgreSQL")
    private val insertSnapshotSql = """
      insert into event_sourcing.snapshots (aggregate_id, version_number, data)
      values ($1, $2, cast($3 as json))""".trimIndent()
  }

  suspend fun loadAggregateEvents(command: LoadAggregateEventsCommand, reply: Reply) {
    var connection: SqlConnection? = null
    try {
      // TODO: error handling
      connection = databasePool.getConnectionA()

      val parameters = Tuple.of(command.aggregateId.id, command.aggregateVersion)
      log.debugIf { "Running event load sql: '$selectEventsSql' with parameters: $parameters" }

      val eventsResultSet: RowSet = connection.queryWithParamsA(selectEventsSql, parameters)

      val events = eventsResultSet.map { databaseMapper.readValue<AggregateEvent>(it.getString(0)) }

      reply(LoadAggregateEventsResponse(command.aggregateId, command.aggregateVersion, events)
        .toRight())
    } catch (ex: Throwable) {
      log.warn("Exception while trying to load Aggregate Events", ex)
      reply(CommandFailedDueToException(ex).toLeft())
    } finally {
      connection?.close()
    }
  }

  suspend fun loadLatestAggregateSnapshot(command: LoadLatestAggregateSnapshotCommand,
    reply: Reply) {
    var connection: SqlConnection? = null
    try {
      // TODO: error handling
      connection = databasePool.getConnectionA()

      val parameters = Tuple.of(command.aggregateId.id)
      log.debugIf { "Running snapshot load sql: '$selectSnapshotSql' with parameters: $parameters" }
      val snapshotResultSet = connection.queryWithParamsA(selectSnapshotSql, parameters)

      val possibleSnapshot: Option<AggregateSnapshot> = snapshotResultSet.map {
        databaseMapper.readValue<AggregateSnapshot>(it.getString(0)) }.firstOrNull().toOption()
      reply(LoadLatestAggregateSnapshotResponse(command.aggregateId, possibleSnapshot).toRight())
    } catch (ex: Throwable) {
      log.warn("Exception while trying to load Aggregate Snapshot", ex)
      reply(CommandFailedDueToException(ex).toLeft())
    } finally {
      connection?.close()
    }
  }

  // TODO: where do we put the retry logic? Here or a higher level? And should it be a
  //  circuit breaker? (probably should)
  suspend fun storeAggregateEvents(command: StoreAggregateEventsCommand, reply: Reply) {
    val events = command.events
    var connection: SqlConnection? = null
    try {
      log.debugIf { "Getting ready to convert events to json" }
      val eventsValues =
        events.map { event: AggregateEvent ->
          val eventSerialized = databaseMapper.writeValueAsString(event)
          Tuple.of(event.aggregateId.id, event.aggregateVersion.version, eventSerialized)
        }
      log.debugIf { "Events converted to Tuples for postgresql" }
      connection = databasePool.getConnectionA()
      connection.batchWithParamsA(insertEventsSql, eventsValues)
      reply(successReplyRight)
    } catch (ex: Throwable) {
      log.warn("Exception while trying to store Aggregate Events", ex)
      reply(CommandFailedDueToException(ex).left())
    } finally {
      connection?.close()
    }
  }

  suspend fun storeAggregateSnapshot(command: StoreAggregateSnapshotCommand, reply: Reply) {
    val snapshot = command.snapshot

    var connection: SqlConnection? = null
    try {
      val snapshotSerialized = databaseMapper.writeValueAsString(snapshot)
      val snapshotValues =
        Tuple.of(snapshot.aggregateId.id, snapshot.aggregateVersion.version, snapshotSerialized)

      log.debugIf {"Insert Snapshot SQL: ***\n$insertSnapshotSql\n*** with parameters $snapshotValues" }

      connection = databasePool.getConnectionA()

      log.debugIf {"connection: $connection"}

      val updateResult = connection.updateWithParamsA(insertSnapshotSql, snapshotValues)
      if (updateResult.rowCount() == 0) {
        reply(ErrorReply(
          "Unable to store aggregate snapshot for snapshot $snapshot, no records were updated",
          this::class).left())
      } else {
        reply(successReplyRight)
      }
    } catch (ex: Throwable) {
      log.warn("Exception while trying to store Aggregate Snapshot", ex)
      reply(CommandFailedDueToException(ex).left())
    } finally {
      connection?.close()
    }
  }
}
