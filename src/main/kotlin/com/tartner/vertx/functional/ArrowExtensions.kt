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

package com.tartner.vertx.functional

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right

fun <L: Any> L.toLeft() = Either.Left(this)
fun <R: Any> R.toRight() = Either.Right(this)

inline fun <R> resultOf(f: () -> R): Either<Exception, R> = try {
  Right(f())
} catch (e: Exception) {
  Left(e)
}

suspend inline fun <L, R, C> Either<L, R>.mapS(crossinline f: suspend (R) -> C)
  : Either<L, C> = foldS({Either.Left(it)}, {Either.Right(f(it))})

suspend inline fun <L, R, C> Either<L, R>.flatMapS(crossinline f: suspend (R) -> Either<L, C>)
  : Either<L, C> = foldS({ Left(it) }, { f(it) })

suspend inline fun <L, R, C> Either<L, R>.foldS(
  crossinline fl: suspend (L) -> C, crossinline fr: suspend (R) -> C): C = when (this) {
    is Either.Right<R> -> fr(b)
    is Either.Left<L> -> fl(a)
  }
