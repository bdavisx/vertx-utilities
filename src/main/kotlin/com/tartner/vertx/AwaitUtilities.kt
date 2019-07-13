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
 *
 */

package com.tartner.vertx

import arrow.core.Either
import com.tartner.vertx.functional.toLeft
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.SqlResult
import io.vertx.sqlclient.Tuple
import java.util.stream.Collectors

val CoroutineVerticle.eventBus: EventBus
  get() = vertx.eventBus()

suspend fun <T> awaitMessageResult(block: (h: Handler<AsyncResult<Message<T>>>) -> Unit) : T {
  val asyncResult = awaitEvent(block)
  if (asyncResult.succeeded()) return asyncResult.result().body()
  else throw asyncResult.cause()
}

data class AwaitAsyncResultFailure(val cause: Throwable): FailureReply

suspend fun <T> awaitMessageEither(block: (
  h: Handler<AsyncResult<Message<Either<FailureReply, T>>>>) -> Unit) : Either<FailureReply, T> {

  val asyncResult = awaitEvent(block)
  return if (asyncResult.succeeded()) {
    val either = asyncResult.result().body()
    either
  } else {
    AwaitAsyncResultFailure(asyncResult.cause()).toLeft()
  }
}

class EitherFailureException(failureReply: FailureReply)
  : RuntimeException(failureReply.toString())

suspend fun Pool.getConnectionA() = awaitResult<SqlConnection> { this.getConnection(it) }

suspend fun SqlConnection.queryA(queryText: String): RowSet =
  awaitResult { this.query(queryText, it) }

suspend fun SqlConnection.queryWithParamsA(queryText: String, params: Tuple): RowSet =
  awaitResult { this.preparedQuery(queryText, params, it) }

suspend fun SqlConnection.updateWithParamsA(queryText: String, params: Tuple)
  : SqlResult<List<Row>> = awaitResult {
  this.preparedQuery(queryText, params, Collectors.toList(), it)
}

suspend fun SqlConnection.batchWithParamsA(queryText: String, params: List<Tuple>)
  : SqlResult<List<Row>> = awaitResult {
  this.preparedBatch(queryText, params, Collectors.toList(), it)
}

