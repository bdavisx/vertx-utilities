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

import com.snapleft.test.utilities.AbstractVertxTest
import com.snapleft.vertx.factories.PercentOfMaximumVerticleInstancesToDeploy
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
import org.slf4j.LoggerFactory

class VerticleDeployerTest: AbstractVertxTest() {
  private val log = LoggerFactory.getLogger(VerticleDeployerTest::class.java)

  @Test(timeout = 2500)
  fun singleDeployment() {
    val context = VertxTestContext()
    log.debug("running singleDeployment")

    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
      try {
        setupVertxKodein(vertx)

        val deployer = VerticleDeployer()

        val futures = deployer.deployVerticles(vertx, listOf(SimpleVerticle()))

        futures.count() shouldBe 1

        val deploymentPromise: Promise<VerticleDeployment> = futures.first()
        val deployment = deploymentPromise.future().await()

        deploymentPromise.future().succeeded() shouldBe true
        deployment.deploymentId.isBlank() shouldBe false
        log.debug("deployment in singleDeployment: $deployment")

        context.completeNow()
      } catch(ex: Throwable) {
        context.failNow(ex)
        fail(ex)
      }
    }}
  }
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
class MultipleDeploymentVerticle(
  val id: String,
  val delegateFactory: DirectCallDelegateFactory
): CoroutineVerticle() {

  private val log = LoggerFactory.getLogger(MultipleDeploymentVerticle::class.java)
  private val directCallDelegate = delegateFactory(directCallAddress(this), this, vertx)

  var counter: Int = 0  // DON'T usually want anything like this in a multi instance verticle

  override suspend fun start() {
    super.start()
  }

  suspend fun increment() = directCallDelegate.act {
    log.debug("Incrementing counter")
    counter++
  }

  override fun toString(): String {
    return "MultipleDeploymentVerticle(localAddress=$id; counter=$counter)"
  }
}
