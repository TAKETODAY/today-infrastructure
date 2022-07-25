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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.aop.interceptor;

import java.lang.reflect.Method;

/**
 * A strategy for handling uncaught exceptions thrown from asynchronous methods.
 *
 * <p>An asynchronous method usually returns a {@link java.util.concurrent.Future}
 * instance that gives access to the underlying exception. When the method does
 * not provide that return type, this handler can be used to manage such
 * uncaught exceptions.
 *
 * @author Stephane Nicoll
 * @author TODAY
 * @since 3.0
 */
@FunctionalInterface
public interface AsyncUncaughtExceptionHandler {

  /**
   * Handle the given uncaught exception thrown from an asynchronous method.
   *
   * @param ex the exception thrown from the asynchronous method
   * @param method the asynchronous method
   * @param params the parameters used to invoked the method
   */
  void handleUncaughtException(Throwable ex, Method method, Object... params);

}
