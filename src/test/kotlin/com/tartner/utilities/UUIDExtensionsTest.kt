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
 *
 */

package com.tartner.utilities

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class UUIDExtensionsTest {
  @Test
  fun toStringFastTest() {
    val uuid = UUID.fromString("4d866a47-9a89-45b0-b6a0-484641e61698")
    val toString: String = uuid.toStringFast()
    toString shouldBe "4d866a47-9a89-45b0-b6a0-484641e61698"
  }

  @Test
  fun toStringFastClearedTest() {
    val uuid = UUID.fromString("4d866a47-9a89-45b0-b6a0-484641e61698")
    val cleared: String = uuid.toStringFastClear()
    cleared shouldBe "4d866a479a8945b0b6a0484641e61698"
  }
}
