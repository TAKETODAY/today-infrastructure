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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.aop.support.AopUtils;
import infra.beans.factory.BeanFactoryAware;
import infra.cache.interceptor.AbstractFallbackCacheOperationSource;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.MethodClassKey;
import infra.util.ReflectionUtils;

/**
 * Abstract implementation of {@link JCacheOperationSource} that caches attributes
 * for methods and implements a fallback policy: 1. specific target method;
 * 2. declaring method.
 *
 * <p>This implementation caches attributes by method after they are first used.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractFallbackCacheOperationSource
 * @since 4.0
 */
public abstract class AbstractFallbackJCacheOperationSource implements JCacheOperationSource {

  /**
   * Canonical value held in cache to indicate no cache operation was
   * found for this method, and we don't need to look again.
   */
  private static final Object NULL_CACHING_MARKER = new Object();

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final Map<MethodClassKey, Object> operationCache = new ConcurrentHashMap<>(1024);

  @Override
  public boolean hasCacheOperation(Method method, @Nullable Class<?> targetClass) {
    return (getCacheOperation(method, targetClass, false) != null);
  }

  @Override
  @Nullable
  public JCacheOperation<?> getCacheOperation(Method method, @Nullable Class<?> targetClass) {
    return getCacheOperation(method, targetClass, true);
  }

  @Nullable
  private JCacheOperation<?> getCacheOperation(Method method, @Nullable Class<?> targetClass, boolean cacheNull) {
    if (ReflectionUtils.isObjectMethod(method)) {
      return null;
    }

    MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
    Object cached = this.operationCache.get(cacheKey);

    if (cached != null) {
      return (cached != NULL_CACHING_MARKER ? (JCacheOperation<?>) cached : null);
    }
    else {
      JCacheOperation<?> operation = computeCacheOperation(method, targetClass);
      if (operation != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Adding cacheable method '{}' with operation: {}", method.getName(), operation);
        }
        this.operationCache.put(cacheKey, operation);
      }
      else if (cacheNull) {
        this.operationCache.put(cacheKey, NULL_CACHING_MARKER);
      }
      return operation;
    }
  }

  private @Nullable JCacheOperation<?> computeCacheOperation(Method method, @Nullable Class<?> targetClass) {
    // Don't allow non-public methods, as configured.
    if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
      return null;
    }

    // Skip setBeanFactory method on BeanFactoryAware.
    if (method.getDeclaringClass() == BeanFactoryAware.class) {
      return null;
    }

    // The method may be on an interface, but we need metadata from the target class.
    // If the target class is null, the method will be unchanged.
    Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

    // First try is the method in the target class.
    JCacheOperation<?> operation = findCacheOperation(specificMethod, targetClass);
    if (operation != null) {
      return operation;
    }
    if (specificMethod != method) {
      // Fallback is to look at the original method.
      operation = findCacheOperation(method, targetClass);
      return operation;
    }
    return null;
  }

  /**
   * Subclasses need to implement this to return the caching operation
   * for the given method, if any.
   *
   * @param method the method to retrieve the operation for
   * @param targetType the target class
   * @return the cache operation associated with this method
   * (or {@code null} if none)
   */
  @Nullable
  protected abstract JCacheOperation<?> findCacheOperation(Method method, @Nullable Class<?> targetType);

  /**
   * Should only public methods be allowed to have caching semantics?
   * <p>The default implementation returns {@code false}.
   */
  protected boolean allowPublicMethodsOnly() {
    return false;
  }

}
