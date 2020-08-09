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

package com.tartner.vertx.cqrs.database

import com.tartner.utilities.debugIf
import com.tartner.vertx.kodein.i
import io.vertx.core.Vertx
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Pool
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import org.slf4j.LoggerFactory

/*
    By inheriting AbstractPool, we can create a Type specific class that can act as a AbstractPool. This
    allows us to actually specify a particular interface as a dependency (e.g. QueryAbstractPool,
    which is a JDBC Client specifically for the query side in CQRS.

    We don't change AbstractPool or add to it in any way. This is specifically about declaring a
    dependency on a specific database connection.
*/

class EventSourcingPool(pool: Pool): AbstractPool(pool)
class AuthenticationPool(pool: Pool): AbstractPool(pool)
class QueryModelPool(pool: Pool): AbstractPool(pool)

val databaseFactoryModule = Kodein.Module("databaseFactoryModule") {
  val environment: MutableMap<String, String> = System.getenv()

  bind<AuthenticationPool>() with singleton { AuthenticationPool(AbstractPool.createPool(
    i(), environment, "databaseQueryModel")) }

  bind<EventSourcingPool>() with singleton { EventSourcingPool(AbstractPool.createPool(
    i(), environment, "databaseEventSourcing")) }

  bind<QueryModelPool>() with singleton { QueryModelPool(AbstractPool.createPool(
    i(), environment, "databaseQueryModel")) }
}

abstract class AbstractPool(private val pool: Pool): Pool by pool {

  companion object {
    val log = LoggerFactory.getLogger(EventSourcingPool::class.java)

    fun createPool(vertx: Vertx, environment: Map<String, String>, environmentNamePrefix: String)
      : Pool {

      val databaseConfiguration = createDatabaseConfiguration(environment, environmentNamePrefix)
      log.debugIf { "prefix: $environmentNamePrefix;  Config: $databaseConfiguration" }

      // Pool Options
      val maxPoolSize = environment.getOrDefault("${environmentNamePrefix}MaxPoolSize","5").toInt()
      val poolOptions = poolOptionsOf(maxSize = maxPoolSize)

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
      value: String) = environment.getValue("$environmentNamePrefix$value")
  }
}
