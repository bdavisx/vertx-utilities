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

package com.snapleft.utilities

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.round

class RandomGenerator {
  private val random get() = ThreadLocalRandom.current()

  fun generateId(): String = UUID.randomUUID().toStringFast()
//    StringBuilder(System.currentTimeMillis().toString(Character.MAX_RADIX))
//      .append(random.nextInt(999999).toString(Character.MAX_RADIX)).toString()

  fun nextInt(range: IntRange): Int {
    return random.nextInt(range.first, range.endInclusive+1)
  }

  fun nextLong(range: LongRange): Long {
    return random.nextLong(range.first, range.endInclusive+1)
  }

  fun nextDouble(range: ClosedFloatingPointRange<Double>, numberOfDecimalPlaces: Int): Double {
    return random.nextDouble(range.start, range.endInclusive).round(numberOfDecimalPlaces)
  }

  fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
  }

  fun dice(numberOfDice: Int, maxDiceValue: Int): List<Int> =
    (1..numberOfDice).map { nextInt(1..maxDiceValue) }

  fun dice(numberOfDice: Int, maxDiceValue: Int, minimumValue: Int): List<Int> {
    var value: List<Int>
    do {
      value = dice(numberOfDice, maxDiceValue)
    } while(value.sum() < minimumValue)

    return value
  }
}
