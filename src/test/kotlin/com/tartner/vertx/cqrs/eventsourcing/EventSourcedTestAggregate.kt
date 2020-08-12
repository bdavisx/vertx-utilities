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
 */

package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.AggregateCommand
import com.tartner.vertx.AggregateEvent
import com.tartner.vertx.AggregateId
import com.tartner.vertx.AggregateVersion
import com.tartner.vertx.CorrelationId
import com.tartner.vertx.newCorrelationId

sealed class TestEventSourcedAggregateCommands(): AggregateCommand
sealed class TestEventSourcedAggregateEvents(): AggregateEvent

data class CreateEventSourcedTestAggregateCommand(
  override val aggregateId: AggregateId, val name: String,
  override val correlationId: CorrelationId = newCorrelationId())
  : TestEventSourcedAggregateCommands(), AggregateCommand

data class ChangeEventSourcedTestAggregateNameCommand(
  override val aggregateId: AggregateId, val name: String,
  override val correlationId: CorrelationId = newCorrelationId())
  : TestEventSourcedAggregateCommands(), AggregateCommand

data class EventSourcedTestAggregateCreated(override val aggregateId: AggregateId,
  override val aggregateVersion: AggregateVersion, val name: String,
  override val correlationId: CorrelationId = newCorrelationId())
  : TestEventSourcedAggregateEvents()

data class EventSourcedTestAggregateNameChanged(override val aggregateId: AggregateId,
  override val aggregateVersion: AggregateVersion, val name: String,
  override val correlationId: CorrelationId = newCorrelationId())
  : TestEventSourcedAggregateEvents()

//data class CreateEventSourcedTestAggregateValidationFailed(val validationIssues: ValidationIssues)
//  : FailureReply

interface TestEventSourcedAggregateQuery {
  suspend fun aggregateIdExists(aggregateId: AggregateId): Boolean
}

//@EventSourcedAggregate
//class EventSourcedTestAggregate(
//  private val aggregateId: AggregateId,
//  private val eventSourcingDelegate: EventSourcingDelegate,
//  private val testEventSourcedAggregateQuery: TestEventSourcedAggregateQuery
//): DirectCallVerticle() {
//
//  private lateinit var name: String
//
//  private fun applyEvents(events: List<TestEventSourcedAggregateEvents>) {
//    events.forEach { event ->
//      when (event) {
//        is EventSourcedTestAggregateCreated -> { name = event.name }
//        is EventSourcedTestAggregateNameChanged -> { name = event.name }
//      }
//    }
//  }
//
//  suspend fun createAggregate(command: CreateEventSourcedTestAggregateCommand)
//    : Either<FailureReply, List<TestEventSourcedAggregateEvents>> = actAndReply {
//
//    val possibleEvents = validateCreateCommand(command).flatMap { validatedCommand ->
//      val aggregateVersion = eventSourcingDelegate.firstVersion(validatedCommand)
//      aggregateVersion.fold(
//        {left -> CreateAggregateVersionFailed(left).createLeft()},
//        {version ->
//          listOf(EventSourcedTestAggregateCreated(aggregateId, version, validatedCommand.name))
//            .createRight()})
//    }
//
//    possibleEvents.mapS {
//      applyEvents(it)
//      eventSourcingDelegate.storeAndPublishEvents(it, eventBus)
//    }
//
//    possibleEvents
//  }
//
//  suspend fun updateName(command: ChangeEventSourcedTestAggregateNameCommand) {
//
//  }
//
//  private suspend fun validateCreateCommand(command: CreateEventSourcedTestAggregateCommand)
//    : Either<CreateEventSourcedTestAggregateValidationFailed, CreateEventSourcedTestAggregateCommand> {
//
//    // TODO: we should sanitize the inputs
//
//    /* Validation: aggregateId must not exist, name must not be blank. */
//    val validation = Validation<CreateEventSourcedTestAggregateCommand> {
//      "aggregateId" {
//        mustBe { testEventSourcedAggregateQuery.aggregateIdExists(command.aggregateId)
//          } ifNot "aggregateId must not already exist"
//        }
//
//        "name" {
//          mustBe { !command.name.isBlank() } ifNot "name: must not be blank"
//        }
//      }
//
//    val possibleValidationIssues: ValidationIssues? = validation.validate(command)
//
//    possibleValidationIssues?.let {
//      return CreateEventSourcedTestAggregateValidationFailed(possibleValidationIssues).createLeft()
//    }
//
//    return command.createRight()
//  }
//
//}
