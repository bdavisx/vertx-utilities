package com.tartner.vertx.kodein

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

  private val defaultConfig = JsonObject()

  fun deployVerticles(vertx: Vertx, verticles: List<Verticle>)
    : List<Future<VerticleDeployment>> = deployVerticles(vertx, verticles, defaultConfig)

  fun deployVerticles(vertx: Vertx, verticles: List<Verticle>, config: JsonObject)
    : List<Future<VerticleDeployment>> {

    val deploymentOptions = DeploymentOptions().setWorker(false).setConfig(config)

    return verticles.map { verticle ->
      val deploymentFuture = Future.future<VerticleDeployment>()
      vertx.deployVerticle(verticle, deploymentOptions) { result ->
        if (result.succeeded()) {
          deploymentFuture.complete(VerticleDeployment(verticle, result.result()))
        } else {
          deploymentFuture.fail(result.cause())
        }
      }
      deploymentFuture
    }
  }
}
