package com.tartner.vertx

typealias IdGenerator = () -> String

interface HasVerticleId {
  val verticleId: String
}
