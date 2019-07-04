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

import com.tartner.vertx.cqrs.database.AbstractSchemaReplacePool
import com.tartner.vertx.updateWithParamsA
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

class DatabaseTestUtilities {
  suspend fun runUpdateSql(sql: String, parameters: Tuple, vertx: Vertx): Int {

    val connection = awaitResult<SqlConnection> { handler ->
      val pool = AbstractSchemaReplacePool.createPool(vertx, System.getenv(), "databaseEventSourcing")
      pool.getConnection(handler)
    }
    val updateResult = connection.updateWithParamsA(sql, parameters)

    return updateResult.rowCount()
  }
}
