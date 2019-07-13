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
 */

package com.tartner.vertx.commands

import com.tartner.vertx.FailureReply

/** Represents the failure of a command due to some general issue, not an exception. */
interface GeneralCommandFailure: FailureReply {
  val message: String
}

/** Represents the failure of a command due to a exception. */
interface CommandFailureDueToException: FailureReply {
  val cause: Throwable
}

data class CommandFailedDueToException(override val cause: Throwable): CommandFailureDueToException
