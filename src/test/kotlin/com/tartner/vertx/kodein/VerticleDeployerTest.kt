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

import com.tartner.vertx.setupVertxKodein
import io.kotlintest.shouldBe
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.provider

@RunWith(VertxUnitRunner::class)
class VerticleDeployerTest {
  private val log = LoggerFactory.getLogger(VerticleDeployerTest::class.java)

  lateinit var vertx: Vertx

  @Before
  fun setup(testContext: TestContext) {
    log.debug("Setting vertx in @before")
    this.vertx = Vertx.vertx()
  }

  @Test(timeout = 2500)
  fun singleDeployment(context: TestContext) {
    log.debug("running singleDeployment")
    val async = context.async()
    vertx.exceptionHandler(context.exceptionHandler())

    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
      try {
        val (vertx, kodein) = setupVertxKodein(listOf(testModule), vertx, context)

        val deployer: VerticleDeployer = kodein.i()

        val futures = deployer.deployVerticles(vertx, listOf(SimpleVerticle()))

        futures.count() shouldBe 1

        val deploymentFuture: Future<VerticleDeployment> = futures.first()
        val deployment = deploymentFuture.await()

        deploymentFuture.succeeded() shouldBe true
        deployment.deploymentId.isBlank() shouldBe false
        log.debug(deployment)

        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }
}

  val testModule = Kodein.Module("VertxDeployerTestModule") {
  bind<SimpleVerticle>() with provider { SimpleVerticle() }
  bind<MultipleDeploymentVerticle>() with factory {id: String -> MultipleDeploymentVerticle(id)}
}

class SimpleVerticle(): CoroutineVerticle()

/*
 * In general, you *don't* want to have any local data that is variable on the multiple deployment
 * verticles, they s/b stateless service verticles. If you do need to access the instance that the
 * code is running in, `it` is passed in as the verticle in the code block - see the `increment`
 * function below. Otherwise `this` is actually captured in the lambda, so it may not be the
 * one you expect when the code runs.
 */
@PercentOfMaximumVerticleInstancesToDeploy(50)
class MultipleDeploymentVerticle(id: String): CoroutineVerticle() {
  private val log = LoggerFactory.getLogger(MultipleDeploymentVerticle::class.java)
  var counter: Int = 0  // DON'T usually want anything like this in a multi instance verticle

  override fun toString(): String {
    return "MultipleDeploymentVerticle(counter=$counter)"
  }
}

