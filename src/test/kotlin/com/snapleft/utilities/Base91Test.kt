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

package de.bwaldvogel.base91

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset
import java.util.HashMap
import java.util.Random

class Base91Test {

  @Test
  @Throws(Exception::class)
  fun testEncodeDecode() {

    val encodeDecodes = HashMap<String, String>()
    encodeDecodes["test"] = "fPNKd"
    encodeDecodes["Never odd or even\n"] = "_O^gp@J`7RztjblLA#_1eHA"
    encodeDecodes["May a moody baby doom a yam?\n"] = "8D9Kc)=/2\$WzeFui#G9Km+<{VT2u9MZil}[A"
    encodeDecodes[""] = ""
    encodeDecodes["a"] = "GB"

    for ((plainText, encodedText) in encodeDecodes) {
      val encode = Base91.encode(plainText.toByteArray(CHARSET))
      val decode = Base91.decode(encode)
      assertEquals(encodedText, String(encode, CHARSET))
      assertEquals(plainText, String(decode, CHARSET))
    }
  }

  @Test
  @Throws(Exception::class)
  fun testRandomEncodeDecode() {

    val random = Random(System.currentTimeMillis())

    var encodedSize = 0
    var plainSize = 0

    var worstEncodingRatio = java.lang.Double.MIN_VALUE
    var bestEncodingRatio = java.lang.Double.MAX_VALUE

    for (i in 0..9999) {
      val bytes = ByteArray(random.nextInt(1000) + 100)
      random.nextBytes(bytes)
      val encode = Base91.encode(bytes)
      val decode = Base91.decode(encode)

      assertArrayEquals(decode, bytes)

      plainSize += bytes.size
      encodedSize += encode.size

      val encodingRatio = encode.size.toDouble() / bytes.size
      worstEncodingRatio = Math.max(worstEncodingRatio, encodingRatio)
      bestEncodingRatio = Math.min(bestEncodingRatio, encodingRatio)
    }

    val encodingRatio = encodedSize.toDouble() / plainSize
    assertTrue(encodingRatio <= worstCaseRatio)
    assertTrue(encodingRatio >= bestCaseRatio)
    println("encoding ratio: $encodingRatio")
    println("worst encoding ratio: $worstEncodingRatio")
    println("best encoding ratio: $bestEncodingRatio")
  }

  companion object {

    private val CHARSET = Charset.forName("ISO-8859-1")
    private val worstCaseRatio = 1.2308
    private val bestCaseRatio = 1.1429
  }
}
