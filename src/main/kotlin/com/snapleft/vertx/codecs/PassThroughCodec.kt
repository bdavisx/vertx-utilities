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
 *
 */

package com.snapleft.vertx.codecs

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec

class PassThroughCodec: MessageCodec<Any, Any> {
  companion object {
    val codecName = PassThroughCodec::class.qualifiedName!!

    const val errorMessage = "Can't send code across the wire!"
  }

  override fun transform(s: Any): Any = s

  override fun systemCodecID(): Byte = -1
  override fun name(): String = codecName

  override fun encodeToWire(buffer: Buffer?, s: Any?) {
    throw UnsupportedOperationException(
      errorMessage)
  }
  override fun decodeFromWire(pos: Int, buffer: Buffer?): Any {
    throw UnsupportedOperationException(
      errorMessage)
  }
}
