package com.snapleft.vertx.dependencyinjection

import com.snapleft.utilities.debugIf
import io.vertx.core.CompositeFuture
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.kodein.di.DirectDIAware
import org.kodein.di.TT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

fun DirectDIAware.v() = directDI.Instance(TT(VerticleKodeinProvider::class))

/** Used to calculate the # of verticles to deploy; default is 1 if this annotation isn't used. */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class PercentOfMaximumVerticleInstancesToDeploy(val percent: Short)

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SpecificNumberOfVerticleInstancesToDeploy(val count: Int)


// TODO: we should deploy the verticles when they are asked for in constructors, before the
//   one representative one is given back

/**
 * We need to create a certain # of verticles that are deployed (or will be deployed),
 * plus we need 1 of the verticles to get injected into the DI system
 */
class VerticleKodeinProvider(
  private val vertx: Vertx,
  private val verticleDeployer: VerticleDeployer,
  private val maximumVerticleInstancesToDeploy: Int,
  private val log: Logger = LoggerFactory.getLogger(VerticleKodeinProvider::class.java)
) {
  private val verticleClassToVerticles = ConcurrentHashMap<KClass<out Verticle>, List<Verticle>>()

  private fun containsVerticleClass(verticleClass: KClass<out Verticle>) =
    verticleClassToVerticles.containsKey(verticleClass)

  // TODO: this needs to be an Either or something "error proof", don't like the `!!`
  /** Note that the verticleClass instance is expected to already exist or there will be an error. */
  private fun verticleForClass(verticleClass: KClass<out Verticle>) =
    verticleClassToVerticles[verticleClass]!!.first()

  // TODO: should this be "synchronized" or is that handled by Kodein?
  fun <T: Verticle> findOrCreate(verticleClass: KClass<T>, factory: () -> T): T {
    if (containsVerticleClass(verticleClass)) { return verticleForClass(verticleClass) as T }

    log.debugIf { "Attempting to create the verticle class: ${verticleClass.qualifiedName}" }

    val numberOfInstances: Int = determineNumberOfVerticleInstances(verticleClass)

    val verticles = (1..numberOfInstances).map { factory() }

    val deployments = verticleDeployer.deployVerticles(vertx, verticles)
    // TODO: this may cause issues since it could run on the eventLoop
    val allDeploymentsFuture =
      runBlocking { CompositeFuture.all(deployments.map { it.future() }).await() }

    // TODO: wtf we gonna do if there's an error on the deployment? That's got to be signalled
    //       somewhere

    verticleClassToVerticles[verticleClass] = verticles
    return verticles.first()
  }

  fun <T: Verticle> determineNumberOfVerticleInstances(verticleClass: KClass<T>): Int {
    val percentageAnnotation = verticleClass.findAnnotation<PercentOfMaximumVerticleInstancesToDeploy>()
    val numberOfInstances: Int = if (percentageAnnotation != null) {
      (percentageAnnotation.percent / 100.0 * maximumVerticleInstancesToDeploy).toInt()
    } else {
      val countAnnotation = verticleClass.findAnnotation<SpecificNumberOfVerticleInstancesToDeploy>()
      if (countAnnotation != null) {
        countAnnotation.count
      } else {
        1
      }
    }

    log.debugIf { "Settings number of instances to ${numberOfInstances} for ${verticleClass.qualifiedName}" }
    return numberOfInstances
  }
}
