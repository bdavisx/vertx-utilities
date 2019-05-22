package com.tartner.vertx.events

import com.tartner.vertx.MessageHandler
import com.tartner.vertx.OpenForTesting
import com.tartner.vertx.VSerializable
import com.tartner.vertx.SuspendableMessageHandler
import com.tartner.vertx.commands.CommandSender
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@OpenForTesting
class EventRegistrar(
  private val eventBus: EventBus,
  private val commandSender: CommandSender
) {

  fun <T: VSerializable>  registerEventHandler(
    eventClass: KClass<T>, handler: MessageHandler<T>): MessageConsumer<T> =
    eventBus.consumer<T>(eventClass.qualifiedName) { message -> handler(message.body()) }

  fun <T: VSerializable> registerEventHandlerSuspendable(
    scope: CoroutineScope, eventClass: KClass<T>, handler: SuspendableMessageHandler<T>) =
    eventBus.consumer<T>(eventClass.qualifiedName) { message ->
      scope.launch { handler(message.body()) } }
}
