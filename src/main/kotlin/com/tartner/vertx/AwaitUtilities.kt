package com.tartner.vertx

import arrow.core.Either
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.functional.toLeft
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.awaitResult

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

suspend fun JDBCClient.getConnectionA() = awaitResult<SQLConnection> { this.getConnection(it) }

suspend fun SQLConnection.queryA(queryText: String): ResultSet =
  awaitResult { this.query(queryText, it) }

suspend fun SQLConnection.queryWithParamsA(queryText: String, params: JsonArray): ResultSet =
  awaitResult { this.queryWithParams(queryText, params, it) }

suspend fun SQLConnection.updateWithParamsA(queryText: String, params: JsonArray): UpdateResult =
  awaitResult { this.updateWithParams(queryText, params, it) }

suspend fun SQLConnection.batchWithParamsA(queryText: String, params: List<JsonArray>): List<Int> =
  awaitResult { this.batchWithParams(queryText, params, it) }

suspend fun <Failure: VSerializable, Success: VSerializable>
  CommandSender.sendA(eventBus: EventBus, message: VSerializable)
    : Message<Either<Failure, Success>> = awaitResult { this.send(message, it) }
