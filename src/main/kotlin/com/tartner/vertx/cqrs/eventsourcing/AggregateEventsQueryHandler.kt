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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tartner.vertx.AggregateEvent
import com.tartner.vertx.CommandHandlingCoroutineDelegate
import com.tartner.vertx.CoroutineDelegateAutoRegister
import com.tartner.vertx.CoroutineDelegateAutoRegistrar
import com.tartner.vertx.Reply
import com.tartner.vertx.SuspendableReplyMessageHandler
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.commands.CommandFailedDueToException
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.cqrs.database.EventSourcingPool
import com.tartner.vertx.debugIf
import com.tartner.vertx.functional.toLeft
import com.tartner.vertx.functional.toRight
import com.tartner.vertx.kodein.PercentOfMaximumVerticleInstancesToDeploy
import com.tartner.vertx.sqlclient.getConnectionAsync
import com.tartner.vertx.sqlclient.queryWithParamsAsync
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.CoroutineScope
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass

@PercentOfMaximumVerticleInstancesToDeploy(100)
class AggregateEventsQueryHandler(
  private val databasePool: EventSourcingPool,
  private val databaseMapper: TypedObjectMapper,
  private val registrar: CoroutineDelegateAutoRegistrar,
  private val log: Logger = LoggerFactory.getLogger(AggregateEventsQueryHandler::class.java)
): CommandHandlingCoroutineDelegate, CoroutineDelegateAutoRegister {
  @Language("PostgreSQL")
  private val selectEventsSql = """
    select data
    from event_sourcing.events
    where aggregate_id = $1 and version_number >= $2
    order by version_number
    """.trimIndent()

  override val commandWithReplyMessageHandlers: Map<KClass<*>, SuspendableReplyMessageHandler<*>> =
    mapOf<KClass<*>, SuspendableReplyMessageHandler<*>>(
      AggregateEventsQuery::class to ::loadAggregateEvents)

  override suspend fun registerCommands(scope: CoroutineScope, commandRegistrar: CommandRegistrar) {
    registrar.registerHandlers(this, scope)
  }

  suspend fun loadAggregateEvents(query: AggregateEventsQuery, reply: Reply) {
    var connection: SqlConnection? = null
    try {
      // TODO: error handling
      connection = databasePool.getConnectionAsync()

      val parameters = Tuple.of(query.aggregateId.id, query.aggregateVersion)
      log.debugIf { "Running event load sql: '$selectEventsSql' with parameters: $parameters" }

      val eventsResultSet: RowSet = connection.queryWithParamsAsync(selectEventsSql, parameters)

      val events = eventsResultSet.map { databaseMapper.readValue<AggregateEvent>(it.getString(0)) }

      reply(AggregateEventsQueryResponse(query.aggregateId, query.aggregateVersion, events)
        .toRight())
    } catch (ex: Exception) {
      log.warn("Exception while trying to load Aggregate Events", ex)
      reply(CommandFailedDueToException(ex).toLeft())
    } finally {
      connection?.close()
    }
  }
}
