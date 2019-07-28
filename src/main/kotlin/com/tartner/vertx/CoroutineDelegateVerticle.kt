/*
 * Copyright (c) 2019, Bill Davis.
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

package com.tartner.vertx

import com.tartner.vertx.commands.CommandRegistrar
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 A verticle that is designed to take an object and delegate command and/or event handling to that
 object, while running in the vertx event loop system.
 */
class EventSourcedAggregateDataVerticle(
  private val commandRegistrar: CommandRegistrar,
  private val eventRegistrar: CommandRegistrar
): CoroutineVerticle() {}
