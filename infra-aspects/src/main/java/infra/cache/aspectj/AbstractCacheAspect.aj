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

package infra.cache.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

import infra.beans.factory.DisposableBean;
import infra.cache.interceptor.CacheAspectSupport;
import infra.cache.interceptor.CacheOperationInvoker;
import infra.cache.interceptor.CacheOperationSource;
import infra.util.ExceptionUtils;

/**
 * Abstract superaspect for AspectJ cache aspects. Concrete subaspects will implement the
 * {@link #cacheMethodExecution} pointcut using a strategy such as Java 5 annotations.
 *
 * <p>Suitable for use inside or outside the Infra IoC container. Set the
 * {@link #setCacheManager cacheManager} property appropriately, allowing use of any cache
 * implementation supported by Infra.
 *
 * <p><b>NB:</b> If a method implements an interface that is itself cache annotated, the
 * relevant Infra cache definition will <i>not</i> be resolved.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
