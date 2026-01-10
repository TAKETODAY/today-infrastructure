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

package infra.cache.jcache.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheInterceptor;
import infra.cache.interceptor.CacheOperationInvoker;
import infra.cache.interceptor.SimpleCacheErrorHandler;
import infra.lang.Assert;
import infra.util.function.SingletonSupplier;

/**
 * AOP Alliance MethodInterceptor for declarative cache
 * management using JSR-107 caching annotations.
 *
 * <p>Derives from the {@link JCacheAspectSupport} class which
 * contains the integration with Framework's underlying caching API.
 * JCacheInterceptor simply calls the relevant superclass method.
 *
 * <p>JCacheInterceptors are thread-safe.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see CacheInterceptor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class JCacheInterceptor extends JCacheAspectSupport implements MethodInterceptor, Serializable {

  /**
   * Construct a new {@code JCacheInterceptor} with the default error handler.
   */
  public JCacheInterceptor() {
  }

  /**
   * Construct a new {@code JCacheInterceptor} with the given error handler.
   *
   * @param errorHandler a supplier for the error handler to use,
   * applying the default error handler if the supplier is not resolvable
   */
  public JCacheInterceptor(@Nullable Supplier<CacheErrorHandler> errorHandler) {
    this.errorHandler = new SingletonSupplier<>(errorHandler, SimpleCacheErrorHandler::new);
  }

  @Override
  @Nullable
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    Method method = invocation.getMethod();

    CacheOperationInvoker aopAllianceInvoker = () -> {
      try {
        return invocation.proceed();
      }
      catch (Throwable ex) {
        throw new CacheOperationInvoker.ThrowableWrapper(ex);
      }
    };

    Object target = invocation.getThis();
    Assert.state(target != null, "Target is required");
    try {
      return execute(aopAllianceInvoker, target, method, invocation.getArguments());
    }
    catch (CacheOperationInvoker.ThrowableWrapper th) {
      throw th.getOriginal();
    }
  }

}
