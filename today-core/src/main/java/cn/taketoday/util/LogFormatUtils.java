/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.util;

import java.util.function.Function;
import java.util.regex.Pattern;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;

/**
 * Utility methods for formatting and logging messages.
 *
 * <p>
 * From Spring
 * </p>
 *
 * @author TODAY 2020/12/6 18:29
 * @since 3.0
 */
public abstract class LogFormatUtils {

  private static final Pattern NEWLINE_PATTERN = Pattern.compile("[\n\r]");
  private static final Pattern CONTROL_CHARACTER_PATTERN = Pattern.compile("\\p{Cc}");

  /**
   * Convenience variant of {@link #formatValue(Object, int, boolean)} that
   * limits the length of a log message to 100 characters and also replaces
   * newline and control characters if {@code limitLength} is set to "true".
   *
   * @param value the value to format
   * @param limitLength whether to truncate the value at a length of 100
   * @return the formatted value
   */
  public static String formatValue(@Nullable Object value, boolean limitLength) {
    return formatValue(value, (limitLength ? 100 : -1), limitLength);
  }

  /**
   * Format the given value via {@code toString()}, quoting it if it is a
   * {@link CharSequence}, truncating at the specified {@code maxLength}, and
   * compacting it into a single line when {@code replaceNewLines} is set.
   *
   * @param value the value to be formatted
   * @param maxLength the max length, after which to truncate, or -1 for unlimited
   * @param replaceNewlinesAndControlCharacters whether to replace newline and
   * control characters with placeholders
   * @return the formatted value
   */
  public static String formatValue(@Nullable Object value, int maxLength, boolean replaceNewlinesAndControlCharacters) {
    if (value == null) {
      return "";
    }
    String result;
    try {
      result = ObjectUtils.nullSafeToString(value);
    }
    catch (Throwable ex) {
      result = ObjectUtils.nullSafeToString(ex);
    }
    if (maxLength != -1) {
      result = StringUtils.truncate(result, maxLength);
    }
    if (replaceNewlinesAndControlCharacters) {
      result = NEWLINE_PATTERN.matcher(result).replaceAll("<EOL>");
      result = CONTROL_CHARACTER_PATTERN.matcher(result).replaceAll("?");
    }
    if (value instanceof CharSequence) {
      result = "\"" + result + "\"";
    }
    return result;
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
   * @param logger the logger to use to log the message
   * @param messageFactory function that accepts a boolean set to the value
   * of {@link Logger#isTraceEnabled()}
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
