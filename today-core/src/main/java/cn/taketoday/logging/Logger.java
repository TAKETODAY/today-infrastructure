/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.logging;

import java.io.Serializable;

import cn.taketoday.util.CollectionUtils;

/**
 * Logger From slf4j
 *
 * @author TODAY <br>
 * 2019-11-03 13:15
 */
public abstract class Logger implements Serializable {
  protected static final String FQCN = Logger.class.getName();

  protected final boolean debugEnabled;

  protected Logger(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  /**
   * Return the name of this <code>Logger</code> instance.
   *
   * @return name of this logger instance
   */
  public abstract String getName();

  /**
   * Is the logger instance enabled for the TRACE level?
   *
   * @return True if this Logger is enabled for the TRACE level, false otherwise.
   */
  public abstract boolean isTraceEnabled();

  /**
   * Log a message at the TRACE level.
   *
   * @param msg the message string to be logged
   */
  public void trace(String msg) {
    logInternal(Level.TRACE, msg, null, null);
  }

  /**
   * Log a message at the TRACE level according to the specified format and
   * argument.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the TRACE level.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  public void trace(String format, Object arg) {
    logInternal(Level.TRACE, format, null, new Object[] { arg });
  }

  /**
   * Log a message at the TRACE level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the TRACE level.
   * </p>
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  public void trace(String format, Object arg1, Object arg2) {
    logInternal(Level.TRACE, format, null, new Object[] { arg1, arg2 });
  }

  /**
   * Log a message at the TRACE level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous string concatenation when the logger is disabled
   * for the TRACE level. However, this variant incurs the hidden (and relatively
   * small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for TRACE. The variants taking
   * {@link #trace(String, Object) one} and {@link #trace(String, Object, Object)
   * two} arguments exist solely in order to avoid this hidden cost.
   * </p>
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  public void trace(String format, Object... arguments) {
    logInternal(arguments, Level.TRACE, format);
  }

  /**
   * Log an exception (throwable) at the TRACE level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  public void trace(String msg, Throwable t) {
    logInternal(Level.TRACE, msg, t);
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  /**
   * Log a message at the DEBUG level.
   *
   * @param msg the message string to be logged
   */
  public void debug(String msg) {
    logInternal(Level.DEBUG, msg, null, null);
  }

  /**
   * Log a message at the DEBUG level according to the specified format and
   * argument.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the DEBUG level.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  public void debug(String format, Object arg) {
    logInternal(Level.DEBUG, format, null, new Object[] { arg });
  }

  /**
   * Log a message at the DEBUG level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the DEBUG level.
   * </p>
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  public void debug(String format, Object arg1, Object arg2) {
    logInternal(Level.DEBUG, format, null, new Object[] { arg1, arg2 });
  }

  /**
   * Log a message at the DEBUG level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous string concatenation when the logger is disabled
   * for the DEBUG level. However, this variant incurs the hidden (and relatively
   * small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for DEBUG. The variants taking
   * {@link #debug(String, Object) one} and {@link #debug(String, Object, Object)
   * two} arguments exist solely in order to avoid this hidden cost.
   * </p>
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  public void debug(String format, Object... arguments) {
    logInternal(arguments, Level.DEBUG, format);
  }

  /**
   * Log an exception (throwable) at the DEBUG level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  public void debug(String msg, Throwable t) {
    logInternal(Level.DEBUG, msg, t);
  }

  /**
   * Is the logger instance enabled for the INFO level?
   *
   * @return True if this Logger is enabled for the INFO level, false otherwise.
   */
  public abstract boolean isInfoEnabled();

  /**
   * Log a message at the INFO level.
   *
   * @param msg the message string to be logged
   */
  public void info(String msg) {
    logInternal(Level.INFO, msg, null, null);
  }

  /**
   * Log a message at the INFO level according to the specified format and
   * argument.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the INFO level.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  public void info(String format, Object arg) {
    logInternal(Level.INFO, format, null, new Object[] { arg });
  }

  /**
   * Log a message at the INFO level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the INFO level.
   * </p>
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  public void info(String format, Object arg1, Object arg2) {
    logInternal(Level.INFO, format, null, new Object[] { arg1, arg2 });
  }

  /**
   * Log a message at the INFO level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous string concatenation when the logger is disabled
   * for the INFO level. However, this variant incurs the hidden (and relatively
   * small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for INFO. The variants taking
   * {@link #info(String, Object) one} and {@link #info(String, Object, Object)
   * two} arguments exist solely in order to avoid this hidden cost.
   * </p>
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  public void info(String format, Object... arguments) {
    logInternal(arguments, Level.INFO, format);
  }

  /**
   * Log an exception (throwable) at the INFO level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  public void info(String msg, Throwable t) {
    logInternal(Level.INFO, msg, t, null);
  }

  /**
   * Is the logger instance enabled for the WARN level?
   *
   * @return True if this Logger is enabled for the WARN level, false otherwise.
   */
  public abstract boolean isWarnEnabled();

  /**
   * Log a message at the WARN level.
   *
   * @param msg the message string to be logged
   */
  public void warn(String msg) {
    logInternal(Level.WARN, msg, null, null);
  }

  /**
   * Log a message at the WARN level according to the specified format and
   * argument.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the WARN level.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  public void warn(String format, Object arg) {
    logInternal(Level.WARN, format, null, new Object[] { arg });
  }

  /**
   * Log a message at the WARN level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous string concatenation when the logger is disabled
   * for the WARN level. However, this variant incurs the hidden (and relatively
   * small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for WARN. The variants taking
   * {@link #warn(String, Object) one} and {@link #warn(String, Object, Object)
   * two} arguments exist solely in order to avoid this hidden cost.
   * </p>
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  public void warn(String format, Object... arguments) {
    logInternal(arguments, Level.WARN, format);
  }

  /**
   * Log a message at the WARN level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the WARN level.
   * </p>
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  public void warn(String format, Object arg1, Object arg2) {
    logInternal(Level.WARN, format, null, new Object[] { arg1, arg2 });
  }

  /**
   * Log an exception (throwable) at the WARN level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  public void warn(String msg, Throwable t) {
    logInternal(Level.WARN, msg, t, null);
  }

  /**
   * Is the logger instance enabled for the ERROR level?
   *
   * @return True if this Logger is enabled for the ERROR level, false otherwise.
   */
  public abstract boolean isErrorEnabled();

  /**
   * Log a message at the ERROR level.
   *
   * @param msg the message string to be logged
   */
  public void error(String msg) {
    logInternal(Level.ERROR, msg, null, null);
  }

  /**
   * Log a message at the ERROR level according to the specified format and
   * argument.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the ERROR level.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  public void error(String format, Object arg) {
    logInternal(Level.ERROR, format, null, new Object[] { arg });
  }

  public void error(String format, Object arg, Throwable throwable) {
    logInternal(Level.ERROR, format, throwable, new Object[] { arg });
  }

  /**
   * Log a message at the ERROR level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous object creation when the logger is disabled for
   * the ERROR level.
   * </p>
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  public void error(String format, Object arg1, Object arg2) {
    logInternal(Level.ERROR, format, null, new Object[] { arg1, arg2 });
  }

  public void error(String format, Object arg1, Object arg2, Throwable throwable) {
    logInternal(Level.ERROR, format, throwable, new Object[] { arg1, arg2 });
  }

  /**
   * Log a message at the ERROR level according to the specified format and
   * arguments.
   * <p/>
   * <p>
   * This form avoids superfluous string concatenation when the logger is disabled
   * for the ERROR level. However, this variant incurs the hidden (and relatively
   * small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for ERROR. The variants taking
   * {@link #error(String, Object) one} and {@link #error(String, Object, Object)
   * two} arguments exist solely in order to avoid this hidden cost.
   * </p>
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  public void error(String format, Object... arguments) {
    logInternal(arguments, Level.ERROR, format);
  }

  /**
   * Log an exception (throwable) at the ERROR level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  public void error(String msg, Throwable t) {
    logInternal(Level.ERROR, msg, t, null);
  }

  //

  /**
   * Logs a message with error log level.
   *
   * @param message log this message
   */
  public void error(Object message) {
    logInternal(Level.ERROR, message, null);
  }

  /**
   * Logs an error with error log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  public void error(Object message, Throwable t) {
    logInternal(Level.ERROR, message, t);
  }

  /**
   * Logs a message with warn log level.
   *
   * @param message log this message
   */
  public void warn(Object message) {
    logInternal(Level.WARN, message, null);
  }

  /**
   * Logs an error with warn log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  public void warn(Object message, Throwable t) {
    logInternal(Level.WARN, message, t);
  }

  /**
   * Logs a message with info log level.
   *
   * @param message log this message
   */
  public void info(Object message) {
    logInternal(Level.INFO, message, null);
  }

  /**
   * Logs an error with info log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  public void info(Object message, Throwable t) {
    logInternal(Level.INFO, message, t);
  }

  /**
   * Logs a message with debug log level.
   *
   * @param message log this message
   */
  public void debug(Object message) {
    logInternal(Level.DEBUG, message, null);
  }

  /**
   * Logs an error with debug log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  public void debug(Object message, Throwable t) {
    logInternal(Level.DEBUG, message, t);
  }

  /**
   * Logs a message with trace log level.
   *
   * @param message log this message
   */
  public void trace(Object message) {
    logInternal(Level.TRACE, message, null);
  }

  /**
   * Logs an error with trace log level.
   *
   * @param message log this message
   * @param t log this cause
   */
  public void trace(Object message, Throwable t) {
    logInternal(Level.TRACE, message, t);
  }

  // internal

  private void logInternal(Object[] arguments, Level trace, String format) {
    Object lastElement = CollectionUtils.lastElement(arguments);
    if (lastElement instanceof Throwable throwable) {
      logInternal(trace, format, throwable, arguments);
    }
    else {
      logInternal(trace, format, null, arguments);
    }
  }

  protected abstract void logInternal(Level level, String msg, Throwable t, Object[] args);

  protected abstract void logInternal(Level level, Object msg, Throwable t);

}
