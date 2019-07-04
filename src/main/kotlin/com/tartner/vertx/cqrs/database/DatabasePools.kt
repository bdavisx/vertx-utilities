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

package com.tartner.vertx.cqrs.database

import com.tartner.vertx.debugIf
import com.tartner.vertx.kodein.i
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Pool
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

/*
    By inheriting JDBCClient, we can create a Type specific class that can act as a JDBCClient. This
    allows us to actually specify a particular interface as a dependency (e.g. QueryJDBCClient,
    which is a JDBC Client specifically for the query side in CQRS.

    We don't change JDBCClient or add to it in any way. This is specifically about declaring a
    dependency on a specific database connection. The only QueryJDBCClient that will be created
    will be the one for Querying.
*/

class EventSourcingPool(pool: Pool, initialSchema: String, newSchema: String): AbstractSchemaReplacePool(pool, initialSchema, newSchema)
class AuthenticationPool(pool: Pool, initialSchema: String, newSchema: String): AbstractSchemaReplacePool(pool, initialSchema, newSchema)
class QueryModelPool(pool: Pool, initialSchema: String, newSchema: String): AbstractSchemaReplacePool(pool, initialSchema, newSchema)

val databaseFactoryModule = Kodein.Module("databaseFactoryModule") {
  val environment: MutableMap<String, String> = System.getenv()

  bind<AuthenticationPool>() with singleton { AuthenticationPool(
    AbstractSchemaReplacePool.createPool(
      i(), environment, "databaseQueryModel"), "query_model", "query_model") }

  bind<EventSourcingPool>() with singleton { EventSourcingPool(
    AbstractSchemaReplacePool.createPool(
      i(), environment, "databaseEventSourcing"), "event_sourcing", "event_sourcing") }

  bind<QueryModelPool>() with singleton { QueryModelPool(
    AbstractSchemaReplacePool.createPool(
      i(), environment, "databaseQueryModel"), "query_model", "query_model") }
}

abstract class AbstractSchemaReplacePool(private val pool: Pool, private val initialSchema: String,
  private val newSchema: String): Pool by pool {

  companion object {
    val log = LoggerFactory.getLogger(EventSourcingPool::class.java)

    fun createPool(vertx: Vertx, environment: Map<String, String>,
      environmentNamePrefix: String): Pool {

      val databaseConfiguration = createDatabaseConfiguration(environment, environmentNamePrefix)
      log.debugIf { "prefix: $environmentNamePrefix;  Config: $databaseConfiguration" }

      // Pool Options
      val poolOptions = poolOptionsOf(maxSize = 5)

      // Create the pool from the data object
      val pool = PgPool.pool(vertx, databaseConfiguration, poolOptions)

      if (pool == null) {
        val message = "Unable to create the Postgresql pool"
        log.error(message)
        // TODO: custom exception or something else?
        throw RuntimeException(message)
      }

      return pool
    }

    private fun createDatabaseConfiguration(configuration: Map<String, String>,
      environmentNamePrefix: String): PgConnectOptions {
      return PgConnectOptions()
        .setPort(configurationValue(configuration, environmentNamePrefix, "Port").toInt())
        .setHost(configurationValue(configuration, environmentNamePrefix, "Host"))
        .setDatabase(configurationValue(configuration, environmentNamePrefix, "Database"))
        .setUser(configurationValue(configuration, environmentNamePrefix, "UserId"))
        .setPassword(configurationValue(configuration, environmentNamePrefix, "Password"))
    }

    private fun configurationValue(environment: Map<String, String>, environmentNamePrefix: String,
      value: String) = environment.getValue(environmentNamePrefix+value)
  }
}



