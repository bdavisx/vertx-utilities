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

package com.snapleft.test.utilities

import com.snapleft.vertx.cqrs.database.AbstractPool
import com.snapleft.vertx.cqrs.database.EventSourcingPool
import com.snapleft.vertx.sqlclient.updateWithParamsAsync
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

data class PreparedQueryCaptures(
  val sqlSlot: CapturingSlot<String>, val tupleSlot: CapturingSlot<Tuple>)

/** Runs a sql statement against against a connection. */
suspend fun runUpdateSql(sql: String, parameters: Tuple, vertx: Vertx,
  environmentNamePrefix: String = "databaseEventSourcing"): Int {
  val connection = awaitResult<SqlConnection> { handler ->
    val pool = AbstractPool.createPool(vertx, System.getenv(), environmentNamePrefix)
    pool.getConnection(handler)
  }
  val updateResult = connection.updateWithParamsAsync(sql, parameters)

  return updateResult.rowCount()
}

/**
 * Prepares a mock EventSourcingPool to "return" a SqlConnection (real or mock). Returns captures
 * that will hold the sql and parameters sent to the query after the call.
 */
fun setupSuccessfulGetConnection(mockDatabasePool: EventSourcingPool, connection: SqlConnection) {
  coEvery { mockDatabasePool.getConnection() } answers {
    Future.succeededFuture(connection)
  }
}

fun setupFailedGetConnection(mockDatabasePool: EventSourcingPool, failureException: Throwable) {
  coEvery { mockDatabasePool.getConnection() } answers {
    Future.failedFuture(failureException)
  }
}

/**
 * Prepares a mockConnection to "return" a RowSet<Row>. Returns captures that will hold
 * the sql and parameters sent to the query.
 */
fun setupSuccessfulPreparedQuery(mockConnection: SqlConnection , sqlResult: RowSet<Row>)
  : PreparedQueryCaptures {
  val sqlSlot: CapturingSlot<String> = slot()
  val tupleSlot: CapturingSlot<Tuple> = slot()

  val mockPreparedQuery = mockk<PreparedQuery<RowSet<Row>>>()

  coEvery {
    mockConnection.preparedQuery(capture(sqlSlot))
  } answers {
    mockPreparedQuery
  }

  coEvery {
    mockPreparedQuery.execute(capture(tupleSlot))
  } answers {
    Future.succeededFuture(sqlResult)
  }

  return PreparedQueryCaptures(sqlSlot, tupleSlot)
}
