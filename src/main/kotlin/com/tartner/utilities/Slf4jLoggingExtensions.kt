package com.tartner.utilities

import org.slf4j.Logger

inline fun Logger.infoIf(messageProvider: () -> String) {
  if(isInfoEnabled) info(messageProvider())
}

inline fun Logger.infoIf(t: Throwable, messageProvider: () -> String) {
  if(isInfoEnabled) info(messageProvider(), t)
}

inline fun Logger.debugIf(messageProvider: () -> String) {
  if(isDebugEnabled) debug(messageProvider())
}

inline fun Logger.debugIf(t: Throwable, messageProvider: () -> String) {
  if(isDebugEnabled) debug(messageProvider(), t)
}

inline fun Logger.traceIf(messageProvider: () -> String) {
  if(isTraceEnabled) trace(messageProvider())
}

inline fun Logger.traceIf(t: Throwable, messageProvider: () -> String) {
  if(isTraceEnabled) trace(messageProvider(), t)
}
