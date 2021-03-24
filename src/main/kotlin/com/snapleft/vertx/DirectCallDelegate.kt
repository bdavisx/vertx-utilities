package com.snapleft.vertx

import com.snapleft.vertx.codecs.PassThroughCodec
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class CodeMessage<T: Any>(val block: suspend () -> T)
class ReturnValueCodeMessage<T: Any>(block: suspend () -> T): CodeMessage<T>(block)
class UnitCodeMessage(block: suspend () -> Unit): CodeMessage<Unit>(block)
class FireAndForgetCodeMessage(block: suspend () -> Unit): CodeMessage<Unit>(block)

typealias DirectCallDelegateFactory =
    (address: String, verticle: CoroutineScope, Vertx) -> DirectCallDelegate

fun createDirectCallDelegate(address: String, coroutineScope: CoroutineScope, vertx: Vertx) =
  DirectCallDelegate(address, coroutineScope, vertx)


/**
 * Use the same id for multiple verticles if you want the calls to be distributed.
 *
 * @param coroutineScope A CoroutineVerticle is a CoroutineScope, so you can typically pass `this`
 *   as the value for this parameter
 */
class DirectCallDelegate(
  val address: String,
  coroutineScope: CoroutineScope,
  vertx: Vertx
  ) {
  private val eventBus = vertx.eventBus()

  init {
    vertx.eventBus().localConsumer<CodeMessage<*>>(address) {
      coroutineScope.launch(vertx.dispatcher()) { runCode(it) }
    }
  }

  companion object {
    val codeDeliveryOptions = DeliveryOptions()
    init { codeDeliveryOptions.codecName = PassThroughCodec.codecName }
  }

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The code runs
   * like coroutines are expected to run, the calling thread awaits on the return, it does not "fire
   * and forget".
   */
  suspend fun act(block: suspend () -> Unit): Unit =
    // we could fire and forget here, but we want the semantics of "imperative" code like coroutines
    // have
    eventBus.request<Unit>(address, UnitCodeMessage(block), codeDeliveryOptions)
      .await().body()

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The code runs
   * like coroutines are expected to run, the calling thread awaits on the return, it does not "fire
   * and forget". The value returned by the block will be the return value for this function.
   */
  suspend fun <T: Any> actAndReply(block: suspend () -> T): T =
    eventBus.request<T>(address, ReturnValueCodeMessage(block), codeDeliveryOptions)
      .await().body()

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The calling
   * thread returns immediately.
   */
  fun fireAndForget(block: suspend () -> Unit) {
    eventBus.send(address, FireAndForgetCodeMessage(block), codeDeliveryOptions)
  }

  private suspend fun runCode(codeMessage: Message<CodeMessage<*>>) {
      // TODO: exceptions?
    val code = codeMessage.body()!!
    when (codeMessage.body()) {
      is UnitCodeMessage -> {
        code.block.invoke()
        // returns a (dummy) value to the calling code, so the await() can continue
        codeMessage.reply(1, codeDeliveryOptions)
      }
      is FireAndForgetCodeMessage -> {
        code.block.invoke()
      }
      is ReturnValueCodeMessage -> {
        codeMessage.reply(code.block.invoke(), codeDeliveryOptions)
      }
    }
  }
}
