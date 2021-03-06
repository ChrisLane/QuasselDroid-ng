package de.kuschku.libquassel.util.compatibility

import de.kuschku.libquassel.util.compatibility.reference.JavaLoggingHandler

abstract class LoggingHandler {
  abstract fun log(logLevel: LogLevel, tag: String, message: String? = null,
                   throwable: Throwable? = null)

  abstract fun isLoggable(logLevel: LogLevel, tag: String): Boolean
  inline fun isLoggable(logLevel: LogLevel, tag: String, f: LogContext.() -> Unit) {
    if (isLoggable(logLevel, tag)) {
      object : LogContext {
        override fun log(message: String?, throwable: Throwable?) {
          this@LoggingHandler.log(logLevel, tag, message, throwable)
        }
      }.f()
    }
  }

  interface LogContext {
    fun log(message: String? = null, throwable: Throwable? = null)
  }

  enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    ASSERT
  }

  companion object {
    val loggingHandlers: MutableSet<LoggingHandler> = mutableSetOf(JavaLoggingHandler)

    inline fun log(logLevel: LoggingHandler.LogLevel, tag: String, message: String?,
                   throwable: Throwable?) {
      LoggingHandler.loggingHandlers
        .filter { it.isLoggable(logLevel, tag) }
        .forEach { it.log(logLevel, tag, message, throwable) }
    }

    inline fun log(logLevel: LoggingHandler.LogLevel, tag: String, throwable: Throwable) =
      log(logLevel, tag, null, throwable)

    inline fun log(logLevel: LoggingHandler.LogLevel, tag: String, message: String) =
      log(logLevel, tag, message, null)
  }
}
