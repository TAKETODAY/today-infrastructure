/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.aop.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import infra.aop.MethodMatcher;
import infra.lang.Assert;

/**
 * Runtime MethodInterceptor
 * <p>
 * use runtime {@link MethodMatcher} to match interceptor can be executed
 *
 * </p>
 *
 * @author TODAY 2021/2/1 19:31
 * @since 3.0
 */
public final class RuntimeMethodInterceptor implements MethodInterceptor {

  private final MethodMatcher methodMatcher;
  private final MethodInterceptor interceptor;

  public RuntimeMethodInterceptor(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
    Assert.notNull(interceptor, "interceptor is required");
    Assert.notNull(methodMatcher, "methodMatcher is required");
    Assert.state(methodMatcher.isRuntime(), "methodMatcher must be a runtime Matcher");

    this.interceptor = interceptor;
    this.methodMatcher = methodMatcher;
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation invocation) throws Throwable {
    if (methodMatcher.matches(invocation)) {
      return interceptor.invoke(invocation);
    }
    else {
      // next in the chain.
      return invocation.proceed();
    }
  }

}
