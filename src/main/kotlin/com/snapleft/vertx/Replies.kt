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

package com.snapleft.vertx

/** This file has standard async replies. */

import arrow.core.Either
import arrow.core.right
import kotlin.reflect.KClass

interface SuccessReply: VMessage
object DefaultSuccessReply: SuccessReply
val successReplyRight = DefaultSuccessReply.right()

interface FailureReply: VMessage
data class ErrorReply(val message: String, val sourceClass: KClass<*>): FailureReply

typealias FailureOrSuccess = Either<FailureReply, SuccessReply>
