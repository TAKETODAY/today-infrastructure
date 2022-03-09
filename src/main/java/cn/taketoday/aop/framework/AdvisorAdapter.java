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

package cn.taketoday.aop.framework;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import cn.taketoday.aop.Advisor;

/**
 * Interface allowing extension to the  AOP framework to allow
 * handling of new Advisors and Advice types.
 *
 * <p>Implementing objects can create AOP Alliance Interceptors from
 * custom advice types, enabling these advice types to be used
 * in the  AOP framework, which uses interception under the covers.
 *
 * <p>There is no need for most  users to implement this interface;
 * do so only if you need to introduce more Advisor or Advice types to .
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 19:36
 * @since 3.0
 */
public interface AdvisorAdapter {

  /**
   * Does this adapter understand this advice object? Is it valid to
   * invoke the {@code getInterceptors} method with an Advisor that
   * contains this advice as an argument?
   *
   * @param advice an Advice such as a BeforeAdvice
   * @return whether this adapter understands the given advice object
   * @see #getInterceptor(Advisor)
   * @see cn.taketoday.aop.BeforeAdvice
   */
  boolean supportsAdvice(Advice advice);

  /**
   * Return an AOP Alliance MethodInterceptor exposing the behavior of
   * the given advice to an interception-based AOP framework.
   * <p>Don't worry about any Pointcut contained in the Advisor;
   * the AOP framework will take care of checking the pointcut.
   *
   * @param advisor the Advisor. The supportsAdvice() method must have
   * returned true on this object
   * @return an AOP Alliance interceptor for this Advisor. There's
   * no need to cache instances for efficiency, as the AOP framework
   * caches advice chains.
   */
  MethodInterceptor getInterceptor(Advisor advisor);

}
