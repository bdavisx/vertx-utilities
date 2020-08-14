package com.tartner.vertx

import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch

sealed class CodeMessage<T: Any, Subtype: DirectCallVerticle<*>>(val block: suspend (Subtype) -> T)
class ReturnValueCodeMessage<T: Any, Subtype: DirectCallVerticle<*>>(block: suspend (Subtype) -> T):CodeMessage<T, Subtype>(block)
class UnitCodeMessage<Subtype: DirectCallVerticle<*>>(block: suspend (Subtype) -> Unit): CodeMessage<Unit, Subtype>(block)
class FireAndForgetCodeMessage<Subtype: DirectCallVerticle<*>>(block: suspend (Subtype) -> Unit): CodeMessage<Unit, Subtype>(block)

/**
 * Use the same id for multiple verticles if you want the calls to be distributed.
 */
open class DirectCallVerticle<Subtype: DirectCallVerticle<Subtype>>(val localAddress: String)
  : CoroutineVerticle() {

  companion object {
    val codeDeliveryOptions = DeliveryOptions()
    init { codeDeliveryOptions.codecName = CodeMessage::class.qualifiedName }

    fun isDirectCallVerticle(jvmType: Class<*>) =
      DirectCallVerticle::class.java.isAssignableFrom(jvmType)
  }

  override suspend fun start() {
    super.start()
    vertx.eventBus().localConsumer<CodeMessage<*, Subtype>>(localAddress,
      { launch(vertx.dispatcher()) { runCode(it) } })
  }

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The code runs
   * like coroutines are expected to run, the calling thread awaits on the return, it does not "fire
   * and forget".
   */
  protected suspend fun act(block: suspend (Subtype) -> Unit) =
    // we could fire and forget here, but we want the semantics of "imperative" code like coroutines have
    eventBus.request<Unit>(localAddress, UnitCodeMessage(block), codeDeliveryOptions).await().body()

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The code runs
   * like coroutines are expected to run, the calling thread awaits on the return, it does not "fire
   * and forget". The value returned by the block will be the return value for this function.
   */
  protected suspend fun <T: Any> actAndReply(block: suspend (Subtype) -> T): T =
    eventBus.request<T>(localAddress, ReturnValueCodeMessage(block), codeDeliveryOptions)
      .await().body()

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The calling
   * thread returns immediately.
   */
  protected fun fireAndForget(block: suspend (Subtype) -> Unit) {
    eventBus.send(localAddress, FireAndForgetCodeMessage(block), codeDeliveryOptions)
  }

  private suspend fun runCode(codeMessage: Message<CodeMessage<*, Subtype>>) {
      // TODO: exceptions?
    val code = codeMessage.body()!!
    when (codeMessage.body()) {
      is UnitCodeMessage -> {
        code.block.invoke(this as Subtype)
        codeMessage.reply(1)
      }
      is FireAndForgetCodeMessage -> {
        code.block.invoke(this as Subtype)
      }
      is ReturnValueCodeMessage -> {
        codeMessage.reply(code.block.invoke(this as Subtype))
      }
    }
  }
}
