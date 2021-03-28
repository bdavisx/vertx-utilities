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
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.module.kotlin.readValue
import com.snapleft.utilities.debugIf
import com.snapleft.vertx.AggregateEvent
import com.snapleft.vertx.AggregateId
import com.snapleft.vertx.AggregateSnapshot
import com.snapleft.vertx.DefaultSuccessReply
import com.snapleft.vertx.DirectCallDelegateFactory
import com.snapleft.vertx.ErrorReply
import com.snapleft.vertx.FailureReply
import com.snapleft.vertx.VCommand
import com.snapleft.vertx.VQuery
import com.snapleft.vertx.codecs.TypedObjectMapper
import com.snapleft.vertx.commands.CommandFailedDueToException
import com.snapleft.vertx.cqrs.database.EventSourcingPool
import com.snapleft.vertx.factories.PercentOfMaximumVerticleInstancesToDeploy
import com.snapleft.vertx.sqlclient.batchWithParamsAsync
import com.snapleft.vertx.sqlclient.queryWithParamsAsync
import com.snapleft.vertx.sqlclient.updateWithParamsAsync
import com.snapleft.vertx.successReplyRight
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class AggregateEventsQuery(val aggregateId: AggregateId, val aggregateVersion: Long): VQuery
data class LatestAggregateSnapshotQuery(val aggregateId: AggregateId): VCommand

data class StoreAggregateEventsCommand(
  val aggregateId: AggregateId, val events: List<AggregateEvent>): VCommand
data class StoreAggregateSnapshotCommand(
  val aggregateId: AggregateId, val snapshot: AggregateSnapshot): VCommand


fun eventSourcingApiVerticleFactory(
  databasePool: EventSourcingPool,
  databaseMapper: TypedObjectMapper,
  directCallDelegateFactory: DirectCallDelegateFactory,
) =
  EventSourcingApiVerticle(databasePool, databaseMapper, directCallDelegateFactory)


@PercentOfMaximumVerticleInstancesToDeploy(100)
class EventSourcingApiVerticle(
  private val databasePool: EventSourcingPool,
  private val databaseMapper: TypedObjectMapper,
  private val directCallDelegateFactory: DirectCallDelegateFactory,
  private val log: Logger = LoggerFactory.getLogger(EventSourcingApiVerticle::class.java)
): CoroutineVerticle() {
  private val thisAddress = EventSourcingApiVerticle::class.qualifiedName!!
  private val directCallDelegate = directCallDelegateFactory(thisAddress, this, vertx)

  @Language("PostgreSQL")
  private val insertEventsSql = """
    insert into event_sourcing.events (aggregate_id, version_number, data)
    values ($1, $2, cast($3 as json))""".trimIndent()

  override suspend fun start() {
    super.start()
  }

  // TODO: where do we put the retry logic? Here or a higher level? And should it be a
  //  circuit breaker? (probably should)
  suspend fun storeAggregateEvents(command: StoreAggregateEventsCommand)
    = directCallDelegate.actAndReply {

    var connection: SqlConnection? = null
    try {
      log.debugIf { "Getting ready to convert events to json" }
      val eventsValues =
        command.events.map { event: AggregateEvent ->
          val eventSerialized = databaseMapper.writeValueAsString(event)
          Tuple.of(event.aggregateId.id, event.aggregateVersion.version, eventSerialized)
        }
      log.debugIf { "Events converted to Tuples for postgresql" }
      connection = databasePool.connection.await()
      connection.batchWithParamsAsync(insertEventsSql, eventsValues)
      successReplyRight
    } catch (ex: Exception) {
      log.warn("Exception while trying to store Aggregate Events", ex)
      CommandFailedDueToException(ex).left()
    } finally {
      connection?.close()
    }
  }

  @Language("PostgreSQL")
  private val selectEventsSql = """
    select data
    from event_sourcing.events
    where aggregate_id = $1 and version_number >= $2
    order by version_number
    """.trimIndent()

  suspend fun loadAggregateEvents(query: AggregateEventsQuery)
    : Either<CommandFailedDueToException, List<AggregateEvent>> = directCallDelegate.actAndReply {
    val (aggregateId: AggregateId, aggregateVersion: Long) = query

    var connection: SqlConnection? = null
    try {
      // TODO: error handling
      connection = databasePool.connection.await()

      val parameters = Tuple.of(aggregateId.id, aggregateVersion)
      log.debugIf { "Running event load sql: '$selectEventsSql' with parameters: $parameters" }

      val eventsResultSet: RowSet<Row> = connection.queryWithParamsAsync(selectEventsSql, parameters)

      val events = eventsResultSet.map { databaseMapper.readValue<AggregateEvent>(it.getString(0)) }

      events.right()
    } catch (ex: Exception) {
      log.warn("Exception while trying to load Aggregate Events", ex)
      CommandFailedDueToException(ex).left()
    } finally {
      connection?.close()
    }
  }

  @Language("PostgreSQL")
  private val insertSnapshotSql = """
      insert into event_sourcing.snapshots (aggregate_id, version_number, data)
      values ($1, $2, cast($3 as json))""".trimIndent()

  suspend fun storeAggregateSnapshot(snapshot: AggregateSnapshot) = directCallDelegate.actAndReply {
    storeAggregateSnapshotActAndReply(StoreAggregateSnapshotCommand(snapshot.aggregateId, snapshot))
  }

  internal suspend fun storeAggregateSnapshotActAndReply(command: StoreAggregateSnapshotCommand)
    : Either<FailureReply, DefaultSuccessReply> {
    val snapshot = command.snapshot

    var connection: SqlConnection? = null
    try {
      val snapshotSerialized = databaseMapper.writeValueAsString(snapshot)
      val snapshotValues = Tuple.of(snapshot.aggregateId.id, snapshot.aggregateVersion.version,
        snapshotSerialized)

      log.debugIf { "Insert Snapshot SQL: ***\n$insertSnapshotSql\n*** with parameters $snapshotValues" }

      connection = databasePool.connection.await()

      log.debugIf { "connection: $connection" }

      val updateResult = connection.updateWithParamsAsync(insertSnapshotSql, snapshotValues)
      if (updateResult.rowCount() == 0) {
        return ErrorReply(
          "Unable to store aggregate snapshot for snapshot $snapshot, no records were updated",
          this::class).left()
      } else {
        return successReplyRight
      }
    } catch (ex: Exception) {
      log.warn("Exception while trying to store Aggregate Snapshot", ex)
      return CommandFailedDueToException(ex).left()
    } finally {
      connection?.close()
    }
  }

  @Language("PostgreSQL")
  private val selectSnapshotSql = """
      select data
      from event_sourcing.snapshots
      where aggregate_id = $1
      order by version_number desc
      limit 1""".trimIndent()

  suspend fun loadLatestAggregateSnapshot(query: LatestAggregateSnapshotQuery)
    : Either<CommandFailedDueToException, AggregateSnapshot?> = directCallDelegate.actAndReply {

    var connection: SqlConnection? = null
    try {
      // TODO: error handling
      connection = databasePool.connection.await()

      val parameters = Tuple.of(query.aggregateId.id)
      log.debugIf { "Running snapshot load sql: '$selectSnapshotSql' with parameters: $parameters" }
      val snapshotResultSet = connection.queryWithParamsAsync(selectSnapshotSql, parameters)

      val possibleSnapshot: AggregateSnapshot? = snapshotResultSet.map {
        databaseMapper.readValue<AggregateSnapshot>(it.getString(0)) }.firstOrNull()
      possibleSnapshot.right()
    } catch (ex: Exception) {
      log.warn("Exception while trying to load Aggregate Snapshot", ex)
      CommandFailedDueToException(ex).left()
    } finally {
      connection?.close()
    }
  }
}
