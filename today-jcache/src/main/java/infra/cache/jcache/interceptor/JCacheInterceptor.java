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

package infra.cache.jcache.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheInterceptor;
import infra.cache.interceptor.CacheOperationInvoker;
import infra.cache.interceptor.SimpleCacheErrorHandler;
import infra.lang.Assert;
import infra.lang.Nullable;
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
