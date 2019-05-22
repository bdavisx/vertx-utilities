package com.tartner.utilities

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.round

class RandomGenerator() {
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
