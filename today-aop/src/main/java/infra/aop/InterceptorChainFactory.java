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

package infra.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.aop.framework.Advised;

/**
 * Factory interface for advisor chains.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface InterceptorChainFactory {

  MethodInterceptor[] EMPTY_INTERCEPTOR = new MethodInterceptor[0];

  /**
   * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
   * for the given advisor chain configuration.
   *
   * @param config the AOP configuration in the form of an Advised object
   * @param method the proxied method
   * @param targetClass the target class (may be {@code null} to indicate a proxy without
   * target object, in which case the method's declaring class is the next best option)
   * @return an array of MethodInterceptors (may also include RuntimeMethodInterceptor)
   */
  MethodInterceptor[] getInterceptors(Advised config, Method method, @Nullable Class<?> targetClass);

}
