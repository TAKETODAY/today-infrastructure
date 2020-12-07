package cn.taketoday.web.utils;

import java.util.function.Function;

import cn.taketoday.context.logger.Logger;

/**
 * Utility methods for formatting and logging messages.
 *
 * <p>
 * From Spring
 * </p>
 *
 * @author TODAY
 * @date 2020/12/6 18:29
 * @since 3.0
 */
public abstract class LogFormatUtils {

  /**
   * Format the given value via {@code toString()}, quoting it if it is a
   * {@link CharSequence}, and possibly truncating at 100 if limitLength is
   * set to true.
   *
   * @param value
   *         the value to format
   * @param limitLength
   *         whether to truncate large formatted values (over 100)
   *
   * @return the formatted value
   */
  public static String formatValue(Object value, boolean limitLength) {
    if (value == null) {
      return "";
    }
    String str;
    if (value instanceof CharSequence) {
      str = "\"" + value + "\"";
    }
    else {
      try {
        str = value.toString();
      }
      catch (Throwable ex) {
        str = ex.toString();
      }
    }
    return (limitLength && str.length() > 100 ? str.substring(0, 100) + " (truncated)..." : str);
  }

  /**
   * Use this to log a message with different levels of detail (or different
   * messages) at TRACE vs DEBUG log levels. Effectively, a substitute for:
   * <pre class="code">
   * if (logger.isDebugEnabled()) {
   *   String str = logger.isTraceEnabled() ? "..." : "...";
   *   if (logger.isTraceEnabled()) {
   *     logger.trace(str);
   *   }
   *   else {
   *     logger.debug(str);
   *   }
   * }
   * </pre>
   *
   * @param logger
   *         the logger to use to log the message
   * @param messageFactory
   *         function that accepts a boolean set to the value
   *         of {@link Logger#isTraceEnabled()}
   */
  public static void traceDebug(Logger logger, Function<Boolean, String> messageFactory) {
    if (logger.isDebugEnabled()) {
      boolean traceEnabled = logger.isTraceEnabled();
      String logMessage = messageFactory.apply(traceEnabled);
      if (traceEnabled) {
        logger.trace(logMessage);
      }
      else {
        logger.debug(logMessage);
      }
    }
  }

}
