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
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tartner.vertx.functional.toRight
import kotlin.reflect.KClass

// MUSTFIX: Docs for these interfaces

typealias MessageHandler<T> = (T) -> Unit
typealias ReplyMessageHandler<T> = (T, Reply) -> Unit

typealias SuspendableMessageHandler<T> = suspend (T) -> Unit
typealias SuspendableReplyMessageHandler<T> = suspend (T, Reply) -> Unit

typealias Reply = (Any) -> Unit


/** Marker interface for object serialization. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
interface VSerializable

interface VMessage: VSerializable

interface VEvent: VMessage
interface ApplicationEvent: VEvent

interface VCommand: VMessage
interface VQuery: VMessage
interface VResponse: VMessage

data class AggregateId(val id: String)

interface HasAggregateId { val aggregateId: AggregateId }
interface HasAggregateVersion: HasAggregateId { val aggregateVersion: Long }

interface AggregateCommand: VCommand, HasAggregateId
interface AggregateEvent: VMessage, HasAggregateId, HasAggregateVersion

interface HasComponentId { val componentId: String }

interface ComponentEvent: VEvent, HasComponentId

interface ComponentSnapshot: ComponentEvent

interface AggregateSnapshot: VSerializable, HasAggregateVersion

interface SuccessReply: VMessage
object DefaultSuccessReply: SuccessReply
val successReplyRight = DefaultSuccessReply.toRight()

interface FailureReply: VMessage
data class ErrorReply(val message: String, val sourceClass: KClass<*>): FailureReply

typealias SuccessOrFailure = Either<FailureReply, SuccessReply>
