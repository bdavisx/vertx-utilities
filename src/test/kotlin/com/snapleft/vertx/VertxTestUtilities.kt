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

package com.snapleft.vertx

import com.snapleft.vertx.codecs.PassThroughCodec
import com.snapleft.vertx.commands.CommandRegistrar
import com.snapleft.vertx.commands.CommandSender
import com.snapleft.vertx.cqrs.database.databaseFactoryModule
import com.snapleft.vertx.dependencyinjection.i
import com.snapleft.vertx.dependencyinjection.vertxUtilitiesModule
import com.snapleft.vertx.events.EventPublisher
import com.snapleft.vertx.events.EventRegistrar
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.Tuple
import org.kodein.di.DI
import org.kodein.di.DirectDI
import org.kodein.di.direct

data class VertxKodeinTestObjects(val vertx: Vertx, val dkodein: DirectDI)

fun testModule() = DI.Module("testModule") {
}


fun setupVertxKodein(modules: Iterable<DI.Module>, vertx: Vertx)
  : VertxKodeinTestObjects {

  val modulesWithVertx =
    mutableListOf(vertxUtilitiesModule(vertx), databaseFactoryModule, testModule())

  modulesWithVertx.addAll(modules)
  val dkodein = DI { modulesWithVertx.forEach { import(it) } }.direct

  vertx.eventBus().registerCodec(PassThroughCodec())

  return VertxKodeinTestObjects(vertx, dkodein)
}

data class TestVertxObjects(
  val vertx: Vertx,
  val kodein: DirectDI,
  val commandSender: CommandSender,
  val verticleDeployer: VerticleDeployer,
  val commandRegistrar: CommandRegistrar,
  val eventRegistrar: EventRegistrar,
  val eventPublisher: EventPublisher,
  val eventBus: EventBus
)

fun createTestVertxObjects(modules: Iterable<DI.Module>, vertx: Vertx, testContext: VertxTestContext)
  : TestVertxObjects {

  val (_, kodein) = setupVertxKodein(modules, vertx)

  return TestVertxObjects(vertx, kodein, kodein.i(), kodein.i(), kodein.i(), kodein.i(),
    kodein.i(), kodein.i())
}

fun tupleShouldBe(tuple: Tuple, expectedTuple: Tuple) {
  println("Checking tuple size match tuple: ${tuple.size()} expectedTuple: ${expectedTuple.size()}")
  tuple.size() shouldBe expectedTuple.size()
  for (i in 0 until tuple.size()) {
    val value = tuple.getValue(i)
    val expectedValue = expectedTuple.getValue(i)
    println("Checking tuple values at $i value: $value; expectedValue: $expectedValue")
    value shouldBe expectedValue
  }
}
