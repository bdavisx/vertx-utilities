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

package com.snapleft.vertx.dependencyinjection

import org.kodein.di.DI
import org.kodein.di.DirectDIAware
import org.kodein.di.direct
import org.kodein.di.generic
import org.slf4j.LoggerFactory

/** Mirror of DKodeinAware.instance(tag: Any? = null). */
inline fun <reified T : Any> DirectDIAware.i(tag: Any? = null) = directDI.Instance<T>(generic(), tag)

inline fun <reified T : Any> DI.i(tag: Any? = null) = direct.Instance<T>(generic(), tag)

inline fun <reified T : Any> logger() = LoggerFactory.getLogger(T::class.java)