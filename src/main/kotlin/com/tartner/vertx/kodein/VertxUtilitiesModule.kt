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

package com.tartner.vertx.kodein

import com.tartner.utilities.RandomGenerator
import com.tartner.vertx.CoroutineDelegateAutoRegistrar
import com.tartner.vertx.CoroutineDelegateVerticleFactory
import com.tartner.vertx.IdGenerator
import com.tartner.vertx.RouterVerticle
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.cqrs.eventsourcing.AggregateEventsQueryHandler
import com.tartner.vertx.cqrs.eventsourcing.LatestAggregateSnapshotQueryHandler
import com.tartner.vertx.cqrs.eventsourcing.StoreAggregateEventsPostgresHandler
import com.tartner.vertx.cqrs.eventsourcing.StoreAggregateSnapshotPostgresHandler
import com.tartner.vertx.events.EventPublisher
import com.tartner.vertx.events.EventRegistrar
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.file.FileSystem
import io.vertx.core.shareddata.SharedData
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

fun vertxUtilitiesModule(vertx: Vertx) = Kodein.Module("vertxUtilitiesModule") {
  bind<Kodein>() with provider { kodein }

  bind<Vertx>() with singleton { vertx }
  bind<EventBus>() with singleton { vertx.eventBus() }
  bind<FileSystem>() with singleton { vertx.fileSystem() }
  bind<SharedData>() with singleton { vertx.sharedData() }

  bind<TypedObjectMapper>() with singleton { TypedObjectMapper.default }
  bind<VerticleDeployer>() with singleton { VerticleDeployer() }

  bind<CommandSender>() with singleton { CommandSender(i()) }
  bind<CommandRegistrar>() with singleton { CommandRegistrar(i(), i()) }

  bind<EventPublisher>() with singleton { EventPublisher(i()) }
  bind<EventRegistrar>() with singleton { EventRegistrar(i(), i()) }

  bind<CoroutineDelegateVerticleFactory>() with singleton { CoroutineDelegateVerticleFactory(i(), i(), i(), i()) }
  bind<CoroutineDelegateAutoRegistrar>() with singleton { CoroutineDelegateAutoRegistrar(i(), i(), i(), i()) }
  bind<KodeinVerticleFactoryVerticle>() with singleton { KodeinVerticleFactoryVerticle(kodein.direct, i(), i(), i(), i()) }

  bind<StoreAggregateEventsPostgresHandler>() with provider { StoreAggregateEventsPostgresHandler(i(), i(), i()) }
  bind<StoreAggregateSnapshotPostgresHandler>() with provider { StoreAggregateSnapshotPostgresHandler(i(), i(), i()) }
  bind<AggregateEventsQueryHandler>() with provider { AggregateEventsQueryHandler(i(), i(), i()) }
  bind<LatestAggregateSnapshotQueryHandler>() with provider { LatestAggregateSnapshotQueryHandler(i(), i(), i()) }

  bind<RandomGenerator>() with singleton { RandomGenerator() }
  bind<IdGenerator>() with singleton { i<RandomGenerator>()::generateId }

  bind<RouterVerticle>() with provider { RouterVerticle(i(), i(), i()) }
}
