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

package com.tartner.test.utilities

import com.tartner.vertx.cqrs.database.AbstractPool
import com.tartner.vertx.cqrs.database.EventSourcingPool
import com.tartner.vertx.updateWithParamsA
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.slot
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.SqlResult
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.impl.command.CommandResponse
import java.util.stream.Collector

data class PreparedQueryCaptures(
  val sqlSlot: CapturingSlot<String>, val tupleSlot: CapturingSlot<Tuple>)

/** Runs a sql statement against against a connection. */
suspend fun runUpdateSql(sql: String, parameters: Tuple, vertx: Vertx,
  environmentNamePrefix: String = "databaseEventSourcing"): Int {
  val connection = awaitResult<SqlConnection> { handler ->
    val pool = AbstractPool.createPool(vertx, System.getenv(), environmentNamePrefix)
    pool.getConnection(handler)
  }
  val updateResult = connection.updateWithParamsA(sql, parameters)

  return updateResult.rowCount()
}

/**
 * Prepares a mock EventSourcingPool to "return" a SqlConnection (real or mock). Returns captures
 * that will hold the sql and parameters sent to the query after the call.
 */
fun setupSuccessfulGetConnection(mockDatabasePool: EventSourcingPool, connection: SqlConnection) {
  val getConnectionSlot = slot<Handler<AsyncResult<SqlConnection>>>()
  coEvery { mockDatabasePool.getConnection(capture(getConnectionSlot)) } answers {
    getConnectionSlot.captured.handle(CommandResponse.success(connection))
  }
}

fun setupFailedGetConnection(mockDatabasePool: EventSourcingPool, failureException: Throwable) {
  val getConnectionSlot = slot<Handler<AsyncResult<SqlConnection>>>()
  coEvery { mockDatabasePool.getConnection(capture(getConnectionSlot)) } answers {
    getConnectionSlot.captured.handle(CommandResponse.failure(failureException))
  }
}

/**
 * Prepares a mockConnection to "return" a SqlResult<List<Row>>. Returns captures that will hold
 * the sql and parameters sent to the query.
 */
fun setupSuccessfulPreparedQuery(mockConnection: SqlConnection , sqlResult: SqlResult<List<Row>>)
  : PreparedQueryCaptures {
  val sqlSlot: CapturingSlot<String> = slot<String>()
  val tupleSlot: CapturingSlot<Tuple> = slot<Tuple>()
  val preparedQuerySlot = slot<Handler<AsyncResult<SqlResult<List<Row>>>>>()

  coEvery {
    mockConnection.preparedQuery(capture(sqlSlot), capture(tupleSlot),
      any<Collector<Row, *, List<Row>>>(), capture(preparedQuerySlot))
  } answers {
    preparedQuerySlot.captured.handle(CommandResponse.success(sqlResult))
    mockConnection
  }

  return PreparedQueryCaptures(sqlSlot, tupleSlot)
}
