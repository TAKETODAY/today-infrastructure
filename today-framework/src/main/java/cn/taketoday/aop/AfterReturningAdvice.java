/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.aop;

import org.aopalliance.intercept.MethodInvocation;

/**
 * After returning advice is invoked only on normal method return, not if an
 * exception is thrown. Such advice can see the return value, but cannot change it.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/20 22:20
 * @see MethodBeforeAdvice
 * @see ThrowsAdvice
 * @since 3.0
 */
public interface AfterReturningAdvice extends AfterAdvice {

  /**
   * Callback after a given method successfully returned.
   *
   * @param returnValue the value returned by the method, if any
   * @param invocation the method invocation join-point
   * @throws Throwable if this object wishes to abort the call.
   * Any exception thrown will be returned to the caller if it's
   * allowed by the method signature. Otherwise the exception
   * will be wrapped as a runtime exception.
   */
  void afterReturning(Object returnValue, MethodInvocation invocation) throws Throwable;

}
