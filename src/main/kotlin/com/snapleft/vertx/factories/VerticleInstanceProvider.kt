package com.snapleft.vertx.factories

import com.snapleft.utilities.debugIf
import com.snapleft.vertx.VerticleDeployer
import io.vertx.core.CompositeFuture
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * TODO: document createDefaultVerticleInstanceProvider
 */
fun createDefaultVerticleInstanceProvider(
  vertx: Vertx,
  verticleDeployer: VerticleDeployer,
  maximumVerticleInstancesToDeploy: Int = 8
) =
  VerticleInstanceProvider(vertx, verticleDeployer, maximumVerticleInstancesToDeploy)


/** Used to calculate the # of verticles to deploy; default is 1 if this annotation isn't used. */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class PercentOfMaximumVerticleInstancesToDeploy(val percent: Short)

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SpecificNumberOfVerticleInstancesToDeploy(val count: Int)

/**
 * We need to create a certain # of verticles that are deployed (or will be deployed),
 * if you're using some kind of dependency injection system for direct call verticles, you would
 * want to somehow add 1 of the instances to the DI "container"
 */
class VerticleInstanceProvider(
  private val vertx: Vertx,
  private val verticleDeployer: VerticleDeployer,
  private val maximumVerticleInstancesToDeploy: Int,
  private val log: Logger = LoggerFactory.getLogger(VerticleInstanceProvider::class.java)
) {
  suspend fun <T: Verticle> createInstances(
    verticleClass: KClass<T>,
    factory: () -> T,
  ): List<T> {
    log.debugIf { "Attempting to create the verticle class: ${verticleClass.qualifiedName}" }

    val numberOfInstances: Int = determineNumberOfVerticleInstances(verticleClass)

    val verticles = (1..numberOfInstances).map { factory() }

    val deployments = verticleDeployer.deployVerticles(vertx, verticles)

    val future = CompositeFuture.all(deployments.map { it.future() })
    val allDeploymentsFuture = future.await()

    // TODO: wtf we gonna do if there's an error on the deployment? That's got to be signalled
    //       somewhere

    return verticles
  }

  fun <T: Verticle> determineNumberOfVerticleInstances(verticleClass: KClass<T>): Int {
    val percentageAnnotation = verticleClass.findAnnotation<PercentOfMaximumVerticleInstancesToDeploy>()
    val numberOfInstances: Int = if (percentageAnnotation != null) {
      maxOf(1, (percentageAnnotation.percent / 100.0 * maximumVerticleInstancesToDeploy).toInt())
    } else {
      val countAnnotation = verticleClass.findAnnotation<SpecificNumberOfVerticleInstancesToDeploy>()
      if (countAnnotation != null) {
        countAnnotation.count
      } else {
        1
      }
    }

    log.debugIf { "Settings number of instances to $numberOfInstances for ${verticleClass.qualifiedName}" }
    return numberOfInstances
  }
}
