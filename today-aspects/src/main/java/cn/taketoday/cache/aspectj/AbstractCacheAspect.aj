/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.cache.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.cache.interceptor.CacheAspectSupport;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;
import cn.taketoday.cache.interceptor.CacheOperationSource;
import cn.taketoday.util.ExceptionUtils;

/**
 * Abstract superaspect for AspectJ cache aspects. Concrete subaspects will implement the
 * {@link #cacheMethodExecution} pointcut using a strategy such as Java 5 annotations.
 *
 * <p>Suitable for use inside or outside the Spring IoC container. Set the
 * {@link #setCacheManager cacheManager} property appropriately, allowing use of any cache
 * implementation supported by Infra.
 *
 * <p><b>NB:</b> If a method implements an interface that is itself cache annotated, the
 * relevant Spring cache definition will <i>not</i> be resolved.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 4.0
 */
public abstract aspect AbstractCacheAspect extends CacheAspectSupport implements DisposableBean {

  protected AbstractCacheAspect() { }

  /**
   * Construct object using the given caching metadata retrieval strategy.
   * @param cos {@link CacheOperationSource} implementation, retrieving Spring cache
   * metadata for each joinpoint.
   */
  protected AbstractCacheAspect(CacheOperationSource... cos) {
    setCacheOperationSources(cos);
  }

  @Override
  public void destroy() {
    clearMetadataCache(); // An aspect is basically a singleton
  }

  @SuppressAjWarnings("adviceDidNotMatch")
  Object around(final Object cachedObject): cacheMethodExecution(cachedObject) {
    MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
    Method method = methodSignature.getMethod();

    CacheOperationInvoker aspectJInvoker = new CacheOperationInvoker() {
      public Object invoke() {
        try {
          return proceed(cachedObject);
        }
        catch (Throwable ex) {
          throw new ThrowableWrapper(ex);
        }
      }
    };

    try {
      return execute(aspectJInvoker, thisJoinPoint.getTarget(), method, thisJoinPoint.getArgs());
    }
    catch (CacheOperationInvoker.ThrowableWrapper th) {
      throw ExceptionUtils.sneakyThrow(th.getOriginal());
    }
  }

  /**
   * Concrete subaspects must implement this pointcut, to identify cached methods.
   */
  protected abstract pointcut cacheMethodExecution(Object cachedObject);

}
