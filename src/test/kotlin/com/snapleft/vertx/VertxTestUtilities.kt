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

package com.snapleft.vertx

import com.snapleft.vertx.codecs.EventBusJacksonJsonCodec
import com.snapleft.vertx.commands.CommandRegistrar
import com.snapleft.vertx.commands.CommandSender
import com.snapleft.vertx.cqrs.database.databaseFactoryModule
import com.snapleft.vertx.events.EventPublisher
import com.snapleft.vertx.events.EventRegistrar
import com.snapleft.vertx.kodein.VerticleDeployer
import com.snapleft.vertx.kodein.i
import com.snapleft.vertx.kodein.vertxUtilitiesModule
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.ext.unit.TestContext
import io.vertx.sqlclient.Tuple
import org.kodein.di.DKodein
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance

data class VertxKodeinTestObjects(val vertx: Vertx, val dkodein: DKodein)

fun testModule() = Kodein.Module("testModule") {
}


fun setupVertxKodein(modules: Iterable<Kodein.Module>, vertx: Vertx, testContext: TestContext)
  : VertxKodeinTestObjects {

  vertx.exceptionHandler(testContext.exceptionHandler())

  val modulesWithVertx =
    mutableListOf(vertxUtilitiesModule(vertx), databaseFactoryModule, testModule())

  modulesWithVertx.addAll(modules)
  val dkodein = Kodein { modulesWithVertx.forEach { import(it) } }.direct

  vertx.eventBus().registerCodec(EventBusJacksonJsonCodec(dkodein.instance()))

  return VertxKodeinTestObjects(vertx, dkodein)
}

data class TestVertxObjects(
  val vertx: Vertx,
  val kodein: DKodein,
  val commandSender: CommandSender,
  val verticleDeployer: VerticleDeployer,
  val commandRegistrar: CommandRegistrar,
  val eventRegistrar: EventRegistrar,
  val eventPublisher: EventPublisher,
  val eventBus: EventBus
)

fun createTestVertxObjects(modules: Iterable<Kodein.Module>, vertx: Vertx, testContext: TestContext)
  : TestVertxObjects {

  val (_, kodein) = setupVertxKodein(modules, vertx, testContext)

  vertx.exceptionHandler(testContext.exceptionHandler())

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
