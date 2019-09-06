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

import arrow.core.left
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
import com.tartner.vertx.kodein.PercentOfMaximumVerticleInstancesToDeploy
import com.tartner.vertx.sqlclient.batchWithParamsAsync
import com.tartner.vertx.sqlclient.getConnectionAsync
import com.tartner.vertx.successReplyRight
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.CoroutineScope
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass

@PercentOfMaximumVerticleInstancesToDeploy(100)
class StoreAggregateEventsPostgresHandler(
  private val databasePool: EventSourcingPool,
  private val databaseMapper: TypedObjectMapper,
  private val registrar: CoroutineDelegateAutoRegistrar,
  private val log: Logger = LoggerFactory.getLogger(StoreAggregateEventsPostgresHandler::class.java)
): CommandHandlingCoroutineDelegate, CoroutineDelegateAutoRegister {
  @Language("PostgreSQL")
  private val insertEventsSql = """
    insert into event_sourcing.events (aggregate_id, version_number, data)
    values ($1, $2, cast($3 as json))""".trimIndent()

  override val commandWithReplyMessageHandlers: Map<KClass<*>, SuspendableReplyMessageHandler<*>> =
    mapOf<KClass<*>, SuspendableReplyMessageHandler<*>>(
      StoreAggregateEventsCommand::class to ::storeAggregateEvents)

  override suspend fun registerCommands(scope: CoroutineScope, commandRegistrar: CommandRegistrar) {
    registrar.registerHandlers(this, scope)
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
      connection = databasePool.getConnectionAsync()
      connection.batchWithParamsAsync(insertEventsSql, eventsValues)
      reply(successReplyRight)
    } catch (ex: Exception) {
      log.warn("Exception while trying to store Aggregate Events", ex)
      reply(CommandFailedDueToException(ex).left())
    } finally {
      connection?.close()
    }
  }
}
