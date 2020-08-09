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

import arrow.core.left
import com.tartner.utilities.debugIf
import com.tartner.vertx.CommandHandlingCoroutineDelegate
import com.tartner.vertx.CoroutineDelegateAutoRegister
import com.tartner.vertx.CoroutineDelegateAutoRegistrar
import com.tartner.vertx.ErrorReply
import com.tartner.vertx.Reply
import com.tartner.vertx.SuspendableReplyMessageHandler
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.commands.CommandFailedDueToException
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.cqrs.database.EventSourcingPool
import com.tartner.vertx.kodein.PercentOfMaximumVerticleInstancesToDeploy
import com.tartner.vertx.sqlclient.getConnectionAsync
import com.tartner.vertx.sqlclient.updateWithParamsAsync
import com.tartner.vertx.successReplyRight
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.CoroutineScope
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

@PercentOfMaximumVerticleInstancesToDeploy(25)
class StoreAggregateSnapshotPostgresHandler(
  private val databasePool: EventSourcingPool,
  private val databaseMapper: TypedObjectMapper,
  private val registrar: CoroutineDelegateAutoRegistrar,
  private val log: Logger = LoggerFactory.getLogger(StoreAggregateEventsPostgresHandler::class.java)
): CommandHandlingCoroutineDelegate, CoroutineDelegateAutoRegister {
  @Language("PostgreSQL")
  private val insertSnapshotSql = """
      insert into event_sourcing.snapshots (aggregate_id, version_number, data)
      values ($1, $2, cast($3 as json))""".trimIndent()

  override val commandWithReplyMessageHandlers: Map<KClass<*>, SuspendableReplyMessageHandler<*>> =
    mapOf(StoreAggregateSnapshotCommand::class to ::storeAggregateSnapshot)

  override suspend fun registerCommands(scope: CoroutineScope, commandRegistrar: CommandRegistrar) {
    registrar.registerHandlers(this, scope)
  }

  suspend fun storeAggregateSnapshot(command: StoreAggregateSnapshotCommand, reply: Reply) {
    val snapshot = command.snapshot

    var connection: SqlConnection? = null
    try {
      val snapshotSerialized = databaseMapper.writeValueAsString(snapshot)
      val snapshotValues =
        Tuple.of(snapshot.aggregateId.id, snapshot.aggregateVersion.version, snapshotSerialized)

      log.debugIf {"Insert Snapshot SQL: ***\n$insertSnapshotSql\n*** with parameters $snapshotValues" }

      connection = databasePool.getConnectionAsync()

      log.debugIf {"connection: $connection"}

      val updateResult = connection.updateWithParamsAsync(insertSnapshotSql, snapshotValues)
      if (updateResult.rowCount() == 0) {
        reply(ErrorReply(
          "Unable to store aggregate snapshot for snapshot $snapshot, no records were updated",
          this::class).left())
      } else {
        reply(successReplyRight)
      }
    } catch (ex: Exception) {
      log.warn("Exception while trying to store Aggregate Snapshot", ex)
      reply(CommandFailedDueToException(ex).left())
    } finally {
      connection?.close()
    }
  }
}
