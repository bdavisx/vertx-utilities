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

import com.tartner.vertx.Reply
import com.tartner.vertx.RouterVerticle
import com.tartner.vertx.VCommand
import com.tartner.vertx.VEvent
import com.tartner.vertx.VResponse
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.debugIf
import com.tartner.vertx.events.EventPublisher
import io.vertx.core.CompositeFuture
import io.vertx.core.Verticle
import io.vertx.core.impl.cpu.CpuCoreSensor
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.kodein.di.DKodein
import org.kodein.di.TT
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/** Used to calculate the # of verticles to deploy; default is 1 if this annotation isn't used. */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class PercentOfMaximumVerticleInstancesToDeploy(val percent: Short)

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SpecificNumberOfVerticleInstancesToDeploy(val count: Int)

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class DependsOnVerticleClasses(val dependencies: Array<KClass<*>>)

/**
 Configures maximum # to deploy for each call to the factory, not the overall maximum for the
 system.
 */
data class ConfigureMaximumNumberOfVerticleInstancesToDeployCommand(val maximumNumber: Int):
  VCommand

/** @see ConfigureMaximumNumberOfVerticleInstancesToDeployCommand */
data class MaximumNumberOfVerticleInstancesToDeployConfiguredEvent(val maximumNumber: Int):
  VEvent

data class DeployVerticleInstancesCommand<T: CoroutineVerticle>(val verticleClass: KClass<T>):
  VCommand
data class DeployVerticleInstancesResponse(val verticleDeployments: List<VerticleDeployment>):
  VResponse

data class VerticleInstancesDeployedEvent(
  val verticleClass: KClass<*>, val numberOfInstancesDeployed: Int): VEvent

/**
* We need to create a certain # of verticles that are deployed based on the annotations above.
 */
class KodeinVerticleFactoryVerticle(
  private val kodein: DKodein,
  private val commandRegistrar: CommandRegistrar,
  private val eventPublisher: EventPublisher,
  private val verticleDeployer: VerticleDeployer
): CoroutineVerticle() {
  private val log = LoggerFactory.getLogger(KodeinVerticleFactoryVerticle::class.java)

  companion object {
    val defaultMaximumInstancesToDeploy = CpuCoreSensor.availableProcessors() * 2
  }

  private var maximumVerticleInstancesToDeploy: Int = defaultMaximumInstancesToDeploy

  override suspend fun start() {
    super.start()

    commandRegistrar.registerCommandHandler(
      ConfigureMaximumNumberOfVerticleInstancesToDeployCommand::class, ::configureMaxInstances)

    commandRegistrar.registerCommandHandler(this, DeployVerticleInstancesCommand::class,
      ::deployVerticles)
  }

  private suspend fun deployVerticles(command: DeployVerticleInstancesCommand<*>, reply: Reply) {
    val verticleClass = command.verticleClass

    log.debugIf { "Attempting to create the verticle class: ${verticleClass.qualifiedName}" }

    val numberOfInstances: Int = determineNumberOfVerticleInstances(verticleClass)

    val verticles: List<CoroutineVerticle> = (1..numberOfInstances).map {
      kodein.AllProviders(TT(verticleClass)).first().invoke() }

    log.debug("Deploying ${verticleClass.qualifiedName}")

    val deploymentFutures = verticleDeployer.deployVerticles(vertx, verticles)
    CompositeFuture.all(deploymentFutures).await()

    if(verticleClass != RouterVerticle::class) {
      deploymentFutures.forEach { log.debug("Deployment Future: ${it}") }
    }

    eventPublisher.publish(VerticleInstancesDeployedEvent(verticleClass, numberOfInstances))

    val response = DeployVerticleInstancesResponse(deploymentFutures.map { it.result() })
    reply(response)
  }

  private fun configureMaxInstances(
    command: ConfigureMaximumNumberOfVerticleInstancesToDeployCommand) {
    log.debugIf { "Changing instances to deploy: old#: $maximumVerticleInstancesToDeploy; new# = ${command.maximumNumber}" }
    maximumVerticleInstancesToDeploy = command.maximumNumber
    eventPublisher.publish(MaximumNumberOfVerticleInstancesToDeployConfiguredEvent(command.maximumNumber))
  }

  private fun <T: Verticle> determineNumberOfVerticleInstances(verticleClass: KClass<T>): Int {
    val percentageAnnotation = verticleClass.findAnnotation<PercentOfMaximumVerticleInstancesToDeploy>()
    val numberOfInstances: Int = if (percentageAnnotation != null) {
      val calculatedInstances =
        (percentageAnnotation.percent / 100.0 * maximumVerticleInstancesToDeploy).toInt()
      max(1, calculatedInstances)
    } else {
      val countAnnotation = verticleClass.findAnnotation<SpecificNumberOfVerticleInstancesToDeploy>()
      if (countAnnotation != null) {
        countAnnotation.count
      } else {
        1
      }
    }

    log.debugIf { "Setting number of instances to ${numberOfInstances} for ${verticleClass.qualifiedName}" }
    return numberOfInstances
  }
}

