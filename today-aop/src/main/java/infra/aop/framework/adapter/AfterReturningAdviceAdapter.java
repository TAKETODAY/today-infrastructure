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

package infra.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import java.io.Serializable;

import infra.aop.Advisor;
import infra.aop.AfterReturningAdvice;

/**
 * Adapter to enable {@link infra.aop.AfterReturningAdvice}
 * to be used in the AOP framework.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
class AfterReturningAdviceAdapter implements AdvisorAdapter, Serializable {

  @Override
  public boolean supportsAdvice(Advice advice) {
    return (advice instanceof AfterReturningAdvice);
  }

  @Override
  public MethodInterceptor getInterceptor(Advisor advisor) {
    AfterReturningAdvice advice = (AfterReturningAdvice) advisor.getAdvice();
    return new AfterReturningAdviceInterceptor(advice);
  }

}
