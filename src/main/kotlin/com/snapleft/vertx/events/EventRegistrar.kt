/*
 * Copyright (c) 2020 Bill Davis.
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

package com.snapleft.vertx.events

import com.snapleft.vertx.MessageHandler
import com.snapleft.vertx.OpenForTesting
import com.snapleft.vertx.SuspendableMessageHandler
import com.snapleft.vertx.commands.CommandSender
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@OpenForTesting
class EventRegistrar(
  private val eventBus: EventBus,
  private val commandSender: CommandSender
) {

  fun <T: Any>  registerEventHandler(
    eventClass: KClass<T>, handler: MessageHandler<T>): MessageConsumer<T> =
    eventBus.consumer(eventClass.qualifiedName) { message -> handler(message.body()) }

  fun <T: Any> registerEventHandlerSuspendable(
    scope: CoroutineScope, eventClass: KClass<T>, handler: SuspendableMessageHandler<T>) =
    eventBus.consumer<T>(eventClass.qualifiedName) { message ->
      scope.launch { handler(message.body()) } }
}
