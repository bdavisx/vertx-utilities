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

package com.tartner.test.utilities

import com.tartner.vertx.cqrs.database.QueryModelClientFactory
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.coroutines.awaitResult

class DatabaseTestUtilities(private val queryFactory: QueryModelClientFactory) {
  suspend fun runUpdateSql(sql: String, parameters: JsonArray, vertx: Vertx,
    configuration: JsonObject): Int {

    val connection = awaitResult<SQLConnection> { handler ->
      val jdbcClient = queryFactory.create(vertx, configuration)
      jdbcClient.getConnection(handler)
    }
    val updateResult = awaitResult<UpdateResult> {connection.updateWithParams(sql, parameters, it)}
    return updateResult.updated
  }
}
