package com.tartner.vertx.commands

import com.tartner.vertx.MessageHandler
import com.tartner.vertx.OpenForTesting
import com.tartner.vertx.ReplyMessageHandler
import com.tartner.vertx.SuspendableMessageHandler
import com.tartner.vertx.SuspendableReplyMessageHandler
import com.tartner.vertx.VSerializable
import com.tartner.vertx.debugIf
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.logging.LoggerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
Class that handles registering for commands in a standardized way based on the cluster node and
command. The nodeId should be unique within the cluster.
 */
@OpenForTesting
class CommandRegistrar(
  val eventBus: EventBus,
  val commandSender: CommandSender
) {
  private val log = LoggerFactory.getLogger(CommandRegistrar::class.java)

  /** Registers the command with address == commandClass.qualifiedName */
  fun <T: VSerializable> registerCommandHandler(commandClass: KClass<T>, handler: MessageHandler<T>)
    = registerCommandHandler(commandClass.qualifiedName!!, handler)

  fun <T: VSerializable> registerCommandHandler(commandClass: KClass<T>,
    handler: ReplyMessageHandler<T>)
    = registerCommandHandler(commandClass.qualifiedName!!, handler)

  /** Registers a handler for T that is only local to this node. */
  fun <T: VSerializable> registerCommandHandler(address: String, handler: MessageHandler<T>)
    : MessageConsumer<T> {
    log.debugIf { "Registering command handler for address: $address" }
    return eventBus.localConsumer<T>(address) { message -> handler(message.body()) }
  }

  fun <T: VSerializable> registerCommandHandler(address: String, handler: ReplyMessageHandler<T>)
    : MessageConsumer<T> {
    log.debugIf { "Registering command handler for address: $address" }
    return eventBus.localConsumer<T>(address) {
      message -> handler(message.body()) { commandSender.reply(message, it) }
    }
  }

  fun <T: VSerializable> registerCommandHandler(
    scope: CoroutineScope, commandClass: KClass<T>, handler: SuspendableMessageHandler<T>)
    : MessageConsumer<T> = registerCommandHandler(scope, commandClass.qualifiedName!!, handler)

  fun <T: VSerializable> registerCommandHandler(
    scope: CoroutineScope, commandClass: KClass<T>, handler: SuspendableReplyMessageHandler<T>)
    : MessageConsumer<T> = registerCommandHandler(scope, commandClass.qualifiedName!!, handler)

//  New registration types: gets T and a MessageReply class. or just T or just reply or nothing.
//  Get rid of the existing. For both suspendable and non.

  fun <T: VSerializable> registerCommandHandler(
    scope: CoroutineScope, address: String, handler: SuspendableMessageHandler<T>)
    : MessageConsumer<T> =
      eventBus.localConsumer<T>(address) { message ->
        scope.launch { handler(message.body()) }
      }

  fun <T: VSerializable> registerCommandHandler(
    scope: CoroutineScope, address: String, handler: SuspendableReplyMessageHandler<T>)
    : MessageConsumer<T> =
      eventBus.localConsumer<T>(address) { message ->
        scope.launch { handler(message.body()) { commandSender.reply(message, it) } }
      }
}

