/*
 * Copyright (c) 2019, Bill Davis.
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

import arrow.core.Option
import com.tartner.vertx.AggregateEvent
import com.tartner.vertx.AggregateId
import com.tartner.vertx.AggregateSnapshot
import com.tartner.vertx.SuccessReply
import com.tartner.vertx.VCommand
import com.tartner.vertx.VQuery

data class AggregateEventsQuery(val aggregateId: AggregateId, val aggregateVersion: Long): VQuery
data class AggregateEventsQueryResponse(val aggregateId: AggregateId, val aggregateVersion: Long,
  val events: List<AggregateEvent>): SuccessReply

data class StoreAggregateEventsCommand(
  val aggregateId: AggregateId, val events: List<AggregateEvent>): VCommand
data class StoreAggregateSnapshotCommand(
  val aggregateId: AggregateId, val snapshot: AggregateSnapshot):VCommand

data class LatestAggregateSnapshotQuery(val aggregateId: AggregateId): VCommand
data class LatestAggregateSnapshotQueryResponse(val aggregateId: AggregateId,
  val possibleSnapshot: Option<AggregateSnapshot>): SuccessReply
