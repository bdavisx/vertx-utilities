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

package com.snapleft.vertx.kodein

import com.snapleft.utilities.debugIf
import io.vertx.core.DeploymentOptions
import io.vertx.core.Promise
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class VerticleDeployment(val instance: Verticle, val deploymentId: String)

class VerticleDeployer {
  private val log: Logger = LoggerFactory.getLogger(VerticleDeployer::class.java)

  var defaultConfig: JsonObject = JsonObject()

  fun deployVerticles(vertx: Vertx, verticles: List<Verticle>)
    : List<Promise<VerticleDeployment>> = deployVerticles(vertx, verticles, defaultConfig)

  fun deployVerticles(vertx: Vertx, verticles: List<Verticle>, config: JsonObject)
    : List<Promise<VerticleDeployment>> {

    val deploymentOptions = DeploymentOptions().setWorker(false).setConfig(config)

    return verticles.map { verticle ->
      val deploymentFuture = Promise.promise<VerticleDeployment>()
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
