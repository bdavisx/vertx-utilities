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

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tartner.utilities.toStringFast
import java.util.UUID

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

data class AggregateId(val id: String): VSerializable
data class AggregateVersion(val version: Long): Comparable<AggregateVersion>, VSerializable {
  override fun compareTo(other: AggregateVersion): Int = version.compareTo(other.version)
}

interface HasAggregateId { val aggregateId: AggregateId }

interface HasAggregateVersion: HasAggregateId, Comparable<HasAggregateVersion> {
  val aggregateVersion: AggregateVersion

  override fun compareTo(other: HasAggregateVersion): Int =
    aggregateVersion.compareTo(other.aggregateVersion)
}

///////////////////////

annotation class EventHandler
interface DomainEvent: VSerializable, HasCorrelationId

interface AggregateEvent: DomainEvent, HasAggregateVersion

/** This indicates an error happened that needs to be handled at a higher/different level. */
interface ErrorEvent: DomainEvent

data class CorrelationId(val id: String)
interface HasCorrelationId {
  val correlationId: CorrelationId
}

fun newId() = UUID.randomUUID().toStringFast()
fun newCorrelationId() = CorrelationId(newId())

annotation class CommandHandler

interface DomainCommand: VSerializable, HasCorrelationId

interface AggregateCommand: DomainCommand, HasAggregateId

interface CommandReply: VSerializable

interface Query: VSerializable, HasCorrelationId
interface QueryReply: VSerializable

interface AggregateSnapshot: VSerializable, HasAggregateVersion
