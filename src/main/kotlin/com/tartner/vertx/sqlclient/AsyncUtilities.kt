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

package com.tartner.vertx.sqlclient

import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

inline suspend fun Pool.getConnectionAsync() =
  awaitResult<SqlConnection> { this.getConnection(it) }

inline suspend fun SqlConnection.queryAsync(queryText: String): RowSet<Row> =
  awaitResult {this.query(queryText).execute(it)}

inline suspend fun SqlConnection.queryWithParamsAsync(queryText: String, params: Tuple): RowSet<Row> =
  awaitResult { this.preparedQuery(queryText).execute(params, it) }

inline suspend fun SqlConnection.updateWithParamsAsync(queryText: String, params: Tuple): RowSet<Row> =
  awaitResult { this.preparedQuery(queryText).execute(params, it) }

inline suspend fun SqlConnection.batchWithParamsAsync(queryText: String, params: List<Tuple>): RowSet<Row> =
  awaitResult { this.preparedQuery(queryText).executeBatch(params, it) }
