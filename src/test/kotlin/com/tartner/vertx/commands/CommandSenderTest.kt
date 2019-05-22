package com.tartner.vertx.commands

import com.natpryce.hamkrest.equalTo
import com.tartner.vertx.VCommand
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.setupVertxKodein
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
    val serializer = dKodein.instance<TypedObjectMapper>()

    vertx.exceptionHandler(testContext.exceptionHandler())

    var receivedCommand: TestCommand? = null
    vertx.eventBus().consumer<TestCommand>(
      TestCommand::class.qualifiedName, { message -> receivedCommand = message.body()
    })

    val command = TestCommand(1, "bdavisx@yahoo.com")
    val sender = dKodein.instance<CommandSender>()
    val context = vertx.getOrCreateContext()
    sender.send(command)

    context.runOnContext({
      com.natpryce.hamkrest.assertion.assertThat(receivedCommand, equalTo(command))
      async.complete()
    })
  }
}

data class TestCommand(val value: Int, val string: String): VCommand
