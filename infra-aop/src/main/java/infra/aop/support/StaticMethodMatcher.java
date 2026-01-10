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

package infra.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import infra.aop.MethodMatcher;
import infra.core.OrderedSupport;

/**
 * Convenient abstract superclass for static method matchers, which don't care
 * about arguments at runtime.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:33
 * @since 3.0
 */
public abstract class StaticMethodMatcher extends OrderedSupport implements MethodMatcher {

  @Override
  public final boolean isRuntime() {
    return false;
  }

  @Override
  public boolean matches(MethodInvocation invocation) {
    // should never be invoked because isRuntime() returns false
    throw new UnsupportedOperationException("Illegal MethodMatcher usage");
  }

}
