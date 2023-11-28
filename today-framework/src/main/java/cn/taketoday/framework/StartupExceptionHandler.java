/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;

import cn.taketoday.lang.Nullable;

/**
 * {@link Thread.UncaughtExceptionHandler} to suppress handling already logged exceptions and
 * dealing with system exit.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/19 22:51
 */
class StartupExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final Set<String> LOG_CONFIGURATION_MESSAGES = Set.of("Logback configuration error detected");

  private static final LoggedExceptionHandlerThreadLocal handler = new LoggedExceptionHandlerThreadLocal();

  @Nullable
  private final Thread.UncaughtExceptionHandler parent;

  private final ArrayList<Throwable> loggedExceptions = new ArrayList<>();

  private int exitCode = 0;

  StartupExceptionHandler(@Nullable Thread.UncaughtExceptionHandler parent) {
    this.parent = parent;
  }

  void registerLoggedException(Throwable exception) {
    this.loggedExceptions.add(exception);
  }

  void registerExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  @Override
  public void uncaughtException(Thread thread, Throwable ex) {
    try {
      if (isPassedToParent(ex) && this.parent != null) {
        this.parent.uncaughtException(thread, ex);
      }
    }
    finally {
      this.loggedExceptions.clear();
      if (this.exitCode != 0) {
        System.exit(this.exitCode);
      }
    }
  }

  private boolean isPassedToParent(Throwable ex) {
    return isLogConfigurationMessage(ex) || !isRegistered(ex);
  }

  /**
   * Check if the exception is a log configuration message, i.e. the log call might not
   * have actually output anything.
   *
   * @param ex the source exception
   * @return {@code true} if the exception contains a log configuration message
   */
  private boolean isLogConfigurationMessage(Throwable ex) {
    if (ex instanceof InvocationTargetException) {
      return isLogConfigurationMessage(ex.getCause());
    }
    String message = ex.getMessage();
    if (message != null) {
      for (String candidate : LOG_CONFIGURATION_MESSAGES) {
        if (message.contains(candidate)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isRegistered(Throwable ex) {
    if (this.loggedExceptions.contains(ex)) {
      return true;
    }
    if (ex instanceof InvocationTargetException) {
      return isRegistered(ex.getCause());
    }
    return false;
  }

  static StartupExceptionHandler forCurrentThread() {
    return handler.get();
  }

  /**
   * Thread local used to attach and track handlers.
   */
  private static class LoggedExceptionHandlerThreadLocal extends ThreadLocal<StartupExceptionHandler> {

    @Override
    protected StartupExceptionHandler initialValue() {
      StartupExceptionHandler handler = new StartupExceptionHandler(
              Thread.currentThread().getUncaughtExceptionHandler());
      Thread.currentThread().setUncaughtExceptionHandler(handler);
      return handler;
    }

  }

}
