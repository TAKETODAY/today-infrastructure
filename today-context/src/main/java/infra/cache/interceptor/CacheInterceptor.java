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

package infra.cache.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.reflect.Method;

import infra.cache.Cache;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * AOP Alliance MethodInterceptor for declarative cache
 * management using the common Framework caching infrastructure
 * ({@link Cache}).
 *
 * <p>Derives from the {@link CacheAspectSupport} class which
 * contains the integration with Framework's underlying caching API.
 * CacheInterceptor simply calls the relevant superclass methods
 * in the correct order.
 *
 * <p>CacheInterceptors are thread-safe.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class CacheInterceptor extends CacheAspectSupport implements MethodInterceptor, Serializable {

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
