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

package com.snapleft.vertx.dependencyinjection

import com.snapleft.test.utilities.AbstractVertxTest
import com.snapleft.vertx.DirectCallVerticle
import com.snapleft.vertx.setupVertxKodein
import io.kotest.matchers.shouldBe
import io.vertx.core.Promise
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.provider
import org.slf4j.LoggerFactory

class VerticleDeployerTest: AbstractVertxTest() {
  private val log = LoggerFactory.getLogger(VerticleDeployerTest::class.java)

  @Test(timeout = 2500)
  fun singleDeployment() {
    val context = VertxTestContext()
    log.debug("running singleDeployment")

    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
      try {
        val (vertx, kodein) = setupVertxKodein(listOf(testModule), vertx)

        val deployer: VerticleDeployer = kodein.i()

        val futures = deployer.deployVerticles(vertx, listOf(SimpleVerticle()))

        futures.count() shouldBe 1

        val deploymentPromise: Promise<VerticleDeployment> = futures.first()
        val deployment = deploymentPromise.future().await()

        deploymentPromise.future().succeeded() shouldBe true
        deployment.deploymentId.isBlank() shouldBe false
        log.debug(deployment.toString())

        context.completeNow()
      } catch(ex: Throwable) {
        fail(ex)
      }
    }}
  }
}

  val testModule = DI.Module("VertxDeployerTestModule") {
  bind<SimpleVerticle>() with provider { SimpleVerticle() }
  bind<MultipleDeploymentVerticle>() with factory {id: String -> MultipleDeploymentVerticle(id) }
}

class SimpleVerticle: CoroutineVerticle()

/*
 * In general, you *don't* want to have any local data that is variable on the multiple deployment
 * verticles, they s/b stateless service verticles. If you do need to access the instance that the
 * code is running in, `it` is passed in as the verticle in the code block - see the `increment`
 * function below. Otherwise `this` is actually captured in the lambda, so it may not be the
 * one you expect when the code runs.
 */
@PercentOfMaximumVerticleInstancesToDeploy(50)
class MultipleDeploymentVerticle(id: String)
  : DirectCallVerticle<MultipleDeploymentVerticle>(id) {
  private val log = LoggerFactory.getLogger(MultipleDeploymentVerticle::class.java)

  var counter: Int = 0  // DON'T usually want anything like this in a multi instance verticle

  suspend fun increment() = act {
    log.debug("Incrementing counter")
    it.counter++
  }

  override fun toString(): String {
    return "MultipleDeploymentVerticle(localAddress=$localAddress; counter=$counter)"
  }
}
