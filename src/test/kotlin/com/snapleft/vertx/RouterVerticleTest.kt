package com.snapleft.vertx

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

// it seems like this should totally be an integration test?

@EnabledIfSystemProperty(named = "integration-tests", matches = "true")
internal class RouterVerticleTest {
  @Test
  fun start() {

  }
}
