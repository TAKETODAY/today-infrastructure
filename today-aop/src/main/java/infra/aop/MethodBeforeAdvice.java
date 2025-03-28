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

package infra.aop;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Advice invoked before a method is invoked. Such advices cannot
 * prevent the method call proceeding, unless they throw a Throwable.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/20 22:16
 * @see AfterReturningAdvice
 * @see ThrowsAdvice
 * @since 3.0
 */
public interface MethodBeforeAdvice extends BeforeAdvice {

  /**
   * Callback before a given method is invoked.
   *
   * @param invocation the method invocation join-point
   * @throws Throwable if this object wishes to abort the call.
   * Any exception thrown will be returned to the caller if it's
   * allowed by the method signature. Otherwise the exception
   * will be wrapped as a runtime exception.
   */
  void before(MethodInvocation invocation) throws Throwable;

}
