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
 */

package com.snapleft.test.utilities

import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.config.configRetrieverOptionsOf
import io.vertx.kotlin.config.configStoreOptionsOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.awaitResult

// TODO: move this to tests, then use the -D below to run the app
object TestConfigurationDefaults {
  fun buildDefaultRetriever(vertx: Vertx): ConfigRetriever {
    // -Dvertx-config-path=/home/bill/src/checklists-dev-configuration.json
    val storeEnvironmentOptions = configStoreOptionsOf(type = "file",
      config = jsonObjectOf("path" to "./dev-configuration.json"))
    val configRetrieverOptions = configRetrieverOptionsOf(stores = listOf(storeEnvironmentOptions))
    return ConfigRetriever.create(vertx, configRetrieverOptions)!!
  }

  suspend fun buildConfiguration(vertx: Vertx): JsonObject {
    val retriever = buildDefaultRetriever(vertx)
    return awaitResult { h -> retriever.getConfig(h) }
  }
}
