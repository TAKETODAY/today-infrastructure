/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import infra.aop.AfterAdvice;
import infra.aop.AfterReturningAdvice;
import infra.lang.Assert;

/**
 * Interceptor to wrap an {@link infra.aop.AfterReturningAdvice}.
 * Used internally by the AOP framework; application developers should not need
 * to use this class directly.
 *
 * @author Rod Johnson
 * @see MethodBeforeAdviceInterceptor
 * @see ThrowsAdviceInterceptor
 */
@SuppressWarnings("serial")
public class AfterReturningAdviceInterceptor implements MethodInterceptor, AfterAdvice, Serializable {

  private final AfterReturningAdvice advice;

  /**
   * Create a new AfterReturningAdviceInterceptor for the given advice.
   *
   * @param advice the AfterReturningAdvice to wrap
   */
  public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
    Assert.notNull(advice, "Advice is required");
    this.advice = advice;
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation mi) throws Throwable {
    Object retVal = mi.proceed();
    this.advice.afterReturning(retVal, mi);
    return retVal;
  }

}
