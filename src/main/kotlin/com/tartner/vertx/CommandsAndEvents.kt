package com.tartner.vertx

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tartner.vertx.functional.toRight
import io.vertx.core.eventbus.Message
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

interface HasAggregateId {
  val aggregateId: AggregateId
}

interface AggregateEvent: VMessage, HasAggregateId

interface HasComponentId {
  val componentId: String
}

interface ComponentEvent: VEvent, HasComponentId

interface ComponentSnapshot: ComponentEvent

interface AggregateSnapshot: VSerializable

object SuccessReply: VMessage
val successReplyRight = SuccessReply.toRight()

interface FailureReply: VMessage
data class ErrorReply(val message: String, val sourceClass: KClass<*>):
  FailureReply
