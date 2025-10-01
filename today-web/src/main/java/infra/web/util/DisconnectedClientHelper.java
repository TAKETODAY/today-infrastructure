/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.util;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.ExceptionUtils;

/**
 * Utility methods to assist with identifying and logging exceptions that
 * indicate the server response connection is lost, for example because the
 * client has gone away. This class helps to identify such exceptions and
 * minimize logging to a single line at DEBUG level, while making the full
 * error stacktrace at TRACE level.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/21 21:12
 */
public class DisconnectedClientHelper {

  private static final Set<String> EXCEPTION_PHRASES =
          Set.of("broken pipe", "connection reset by peer");

  private static final Set<String> EXCEPTION_TYPE_NAMES =
          Set.of("AbortedException", "ClientAbortException", "EOFException", "EofException");

  private static final Set<Class<?>> CLIENT_EXCEPTION_TYPES = new HashSet<>(2);

  static {
    try {
      ClassLoader classLoader = DisconnectedClientHelper.class.getClassLoader();
      CLIENT_EXCEPTION_TYPES.add(ClassUtils.forName(
              "infra.web.client.RestClientException", classLoader));
      CLIENT_EXCEPTION_TYPES.add(ClassUtils.forName(
              "infra.web.client.reactive.WebClientException", classLoader));
    }
    catch (ClassNotFoundException ex) {
      // ignore
    }
  }

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
   * </ul>
   */
  public static boolean isClientDisconnectedException(@Nullable Throwable ex) {
    if (ex == null) {
      return false;
    }
    Throwable currentEx = ex;
    Throwable lastEx = null;
    while (currentEx != null && currentEx != lastEx) {
      // Ignore onward connection issues to other servers (500 error)
      for (Class<?> exceptionType : CLIENT_EXCEPTION_TYPES) {
        if (exceptionType.isInstance(currentEx)) {
          return false;
        }
      }
      if (EXCEPTION_TYPE_NAMES.contains(currentEx.getClass().getSimpleName())) {
        return true;
      }
      lastEx = currentEx;
      currentEx = currentEx.getCause();
    }

    String message = ExceptionUtils.getMostSpecificCause(ex).getMessage();
    if (message != null) {
      String text = message.toLowerCase(Locale.ROOT);
      for (String phrase : EXCEPTION_PHRASES) {
        if (text.contains(phrase)) {
          return true;
        }
      }
    }

    return false;
  }

}
