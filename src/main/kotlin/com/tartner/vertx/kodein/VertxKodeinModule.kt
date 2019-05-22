package com.tartner.vertx.kodein

import com.tartner.utilities.RandomGenerator
import com.tartner.vertx.IdGenerator
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.commands.CommandSender
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

fun vertxKodeinModule(vertx: Vertx) = Kodein.Module("vertxKodeinModule") {
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

  bind<KodeinVerticleFactoryVerticle>() with singleton { KodeinVerticleFactoryVerticle(kodein.direct, i(), i(), i()) }

  bind<RandomGenerator>() with singleton { RandomGenerator() }
  bind<IdGenerator>() with singleton { i<RandomGenerator>()::generateId }
}
