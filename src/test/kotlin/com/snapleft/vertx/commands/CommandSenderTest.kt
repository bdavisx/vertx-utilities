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

package com.snapleft.vertx.commands

import com.natpryce.hamkrest.equalTo
import com.snapleft.vertx.VCommand
import com.snapleft.vertx.setupVertxKodein
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.generic.instance

@RunWith(VertxUnitRunner::class)
class CommandSenderTest {
  @get:Rule
  var rule = RunTestOnContext()

  @Before
  fun setup(testContext: TestContext) {
  }

  @Test
  fun testItShouldSendACommandCorrectly(testContext: TestContext) {
    val (vertx, dKodein) = setupVertxKodein(listOf(), rule.vertx(), testContext)

    val async = testContext.async()

    vertx.exceptionHandler(testContext.exceptionHandler())

    var receivedCommand: TestCommand? = null
    vertx.eventBus().consumer<TestCommand>(
      TestCommand::class.qualifiedName) { message -> receivedCommand = message.body()
    }

    val command = TestCommand(1, "bdavisx@yahoo.com")
    val sender = dKodein.instance<CommandSender>()
    val context = vertx.getOrCreateContext()
    sender.send(command)

    context.runOnContext {
      com.natpryce.hamkrest.assertion.assertThat(receivedCommand, equalTo(command))
      async.complete()
    }
  }
}

data class TestCommand(val value: Int, val string: String): VCommand
