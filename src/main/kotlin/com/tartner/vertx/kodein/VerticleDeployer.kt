/*
 * Copyright (c) 2019 the original author or authors.
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

import com.tartner.vertx.debugIf
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

data class VerticleDeployment(val instance: Verticle, val deploymentId: String)

class VerticleDeployer {
  private val log: Logger = LoggerFactory.getLogger(VerticleDeployer::class.java)

  var defaultConfig: JsonObject = JsonObject()

  fun deployVerticles(vertx: Vertx, verticles: List<Verticle>)
    : List<Future<VerticleDeployment>> = deployVerticles(vertx, verticles, defaultConfig)

  fun deployVerticles(vertx: Vertx, verticles: List<Verticle>, config: JsonObject)
    : List<Future<VerticleDeployment>> {

    val deploymentOptions = DeploymentOptions().setWorker(false).setConfig(config)

    return verticles.map { verticle ->
      val deploymentFuture = Future.future<VerticleDeployment>()
      vertx.deployVerticle(verticle, deploymentOptions) { result ->
        if (result.succeeded()) {
          log.debugIf { "Successful deployment of $verticle" }
          deploymentFuture.complete(VerticleDeployment(verticle, result.result()))
        } else {
          val failureCause = result.cause()
          log.error("Failure deploying verticle ($verticle); cause: $failureCause")
          deploymentFuture.fail(failureCause)
        }
      }
      deploymentFuture
    }
  }
}
