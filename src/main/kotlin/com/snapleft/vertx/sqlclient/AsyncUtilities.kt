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

package com.snapleft.vertx.sqlclient

import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

suspend inline fun SqlConnection.queryAsync(queryText: String): RowSet<Row> =
  this.query(queryText).execute().await()

suspend inline fun SqlConnection.queryWithParamsAsync(queryText: String, params: Tuple): RowSet<Row> =
  this.preparedQuery(queryText).execute(params).await()

suspend inline fun SqlConnection.updateWithParamsAsync(queryText: String, params: Tuple): RowSet<Row> =
  this.preparedQuery(queryText).execute(params).await()

suspend inline fun SqlConnection.batchWithParamsAsync(queryText: String, params: List<Tuple>): RowSet<Row> =
  this.preparedQuery(queryText).executeBatch(params).await()
