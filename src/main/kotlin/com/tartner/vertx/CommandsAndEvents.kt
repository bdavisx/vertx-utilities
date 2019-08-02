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

interface AggregateCommand: VCommand, HasAggregateId
interface AggregateEvent: VMessage, HasAggregateId, HasAggregateVersion

interface AggregateSnapshot: VSerializable, HasAggregateVersion

