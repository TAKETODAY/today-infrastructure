/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.util;

import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ExceptionUtils;

/**
 * Utility methods to assist with identifying and logging exceptions that indicate
 * the client has gone away. Such exceptions fill logs with unnecessary stack
 * traces. The utility methods help to log a single line message at DEBUG level,
 * and a full stacktrace at TRACE level.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/21 21:12
 */
public class DisconnectedClientHelper {

  private static final Set<String> EXCEPTION_PHRASES =
          Set.of("broken pipe", "connection reset");

  private static final Set<String> EXCEPTION_TYPE_NAMES =
          Set.of("AbortedException", "ClientAbortException", "EOFException", "EofException");

  private final Logger logger;

  public DisconnectedClientHelper(String logCategory) {
    Assert.notNull(logCategory, "'logCategory' is required");
    this.logger = LoggerFactory.getLogger(logCategory);
  }

  /**
   * Check via  {@link #isClientDisconnectedException} if the exception
   * indicates the remote client disconnected, and if so log a single line
   * message when DEBUG is on, and a full stacktrace when TRACE is on for
   * the configured logger.
   */
  public boolean checkAndLogClientDisconnectedException(Throwable ex) {
    if (isClientDisconnectedException(ex)) {
      if (logger.isTraceEnabled()) {
        logger.trace("Looks like the client has gone away", ex);
      }
      else if (logger.isDebugEnabled()) {
        logger.debug("Looks like the client has gone away: {} (For a full stack trace, set the log category '{}' to TRACE level.)", ex, logger);
      }
      return true;
    }
    return false;
  }

  /**
   * Whether the given exception indicates the client has gone away.
   * <p>Known cases covered:
   * <ul>
   * <li>ClientAbortException or EOFException for Tomcat
   * <li>EofException for Jetty
   * <li>IOException "Broken pipe" or "connection reset by peer"
   * <li>SocketException "Connection reset"
   * </ul>
   */
  public static boolean isClientDisconnectedException(Throwable ex) {
    String message = ExceptionUtils.getMostSpecificCause(ex).getMessage();
    if (message != null) {
      String text = message.toLowerCase();
      for (String phrase : EXCEPTION_PHRASES) {
        if (text.contains(phrase)) {
          return true;
        }
      }
    }
    return EXCEPTION_TYPE_NAMES.contains(ex.getClass().getSimpleName());
  }

}
