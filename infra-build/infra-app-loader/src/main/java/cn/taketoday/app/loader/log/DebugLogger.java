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

package cn.taketoday.app.loader.log;

/**
 * Simple logger class used for {@link System#err} debugging.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public abstract sealed class DebugLogger {

  private static final String ENABLED_PROPERTY = "loader.debug";

  private static final DebugLogger disabled;

  static {
    disabled = Boolean.getBoolean(ENABLED_PROPERTY) ? null : new DisabledDebugLogger();
  }

  /**
   * Log a message.
   *
   * @param message the message to log
   */
  public abstract void log(String message);

  /**
   * Log a formatted message.
   *
   * @param message the message to log
   * @param arg1 the first format argument
   */
  public abstract void log(String message, Object arg1);

  /**
   * Log a formatted message.
   *
   * @param message the message to log
   * @param arg1 the first format argument
   * @param arg2 the second format argument
   */
  public abstract void log(String message, Object arg1, Object arg2);

  /**
   * Log a formatted message.
   *
   * @param message the message to log
   * @param arg1 the first format argument
   * @param arg2 the second format argument
   * @param arg3 the third format argument
   */
  public abstract void log(String message, Object arg1, Object arg2, Object arg3);

  /**
   * Log a formatted message.
   *
   * @param message the message to log
   * @param arg1 the first format argument
   * @param arg2 the second format argument
   * @param arg3 the third format argument
   * @param arg4 the fourth format argument
   */
  public abstract void log(String message, Object arg1, Object arg2, Object arg3, Object arg4);

  /**
   * Get a {@link DebugLogger} to log messages for the given source class.
   *
   * @param sourceClass the source class
   * @return a {@link DebugLogger} instance
   */
  public static DebugLogger get(Class<?> sourceClass) {
    return (disabled != null) ? disabled : new SystemErrDebugLogger(sourceClass);
  }

  /**
   * {@link DebugLogger} used for disabled logging that does nothing.
   */
  private static final class DisabledDebugLogger extends DebugLogger {

    @Override
    public void log(String message) {
    }

    @Override
    public void log(String message, Object arg1) {
    }

    @Override
    public void log(String message, Object arg1, Object arg2) {
    }

    @Override
    public void log(String message, Object arg1, Object arg2, Object arg3) {
    }

    @Override
    public void log(String message, Object arg1, Object arg2, Object arg3, Object arg4) {
    }

  }

  /**
   * {@link DebugLogger} that prints messages to {@link System#err}.
   */
  private static final class SystemErrDebugLogger extends DebugLogger {

    private final String prefix;

    SystemErrDebugLogger(Class<?> sourceClass) {
      this.prefix = "LOADER: " + sourceClass + " : ";
    }

    @Override
    public void log(String message) {
      print(message);
    }

    @Override
    public void log(String message, Object arg1) {
      print(message.formatted(arg1));
    }

    @Override
    public void log(String message, Object arg1, Object arg2) {
      print(message.formatted(arg1, arg2));
    }

    @Override
    public void log(String message, Object arg1, Object arg2, Object arg3) {
      print(message.formatted(arg1, arg2, arg3));
    }

    @Override
    public void log(String message, Object arg1, Object arg2, Object arg3, Object arg4) {
      print(message.formatted(arg1, arg2, arg3, arg4));
    }

    private void print(String message) {
      System.err.println(this.prefix + message);
    }

  }

}
