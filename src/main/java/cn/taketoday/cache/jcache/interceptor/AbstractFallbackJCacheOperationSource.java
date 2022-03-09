/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.cache.interceptor.AbstractFallbackCacheOperationSource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.MethodClassKey;

/**
 * Abstract implementation of {@link JCacheOperationSource} that caches attributes
 * for methods and implements a fallback policy: 1. specific target method;
 * 2. declaring method.
 *
 * <p>This implementation caches attributes by method after they are first used.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see AbstractFallbackCacheOperationSource
 * @since 4.0
 */
public abstract class AbstractFallbackJCacheOperationSource implements JCacheOperationSource {

  /**
   * Canonical value held in cache to indicate no caching attribute was
   * found for this method and we don't need to look again.
   */
  private static final Object NULL_CACHING_ATTRIBUTE = new Object();

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final Map<MethodClassKey, Object> cache = new ConcurrentHashMap<>(1024);

  @Override
  public JCacheOperation<?> getCacheOperation(Method method, @Nullable Class<?> targetClass) {
    MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
    Object cached = this.cache.get(cacheKey);

    if (cached != null) {
      return (cached != NULL_CACHING_ATTRIBUTE ? (JCacheOperation<?>) cached : null);
    }
    else {
      JCacheOperation<?> operation = computeCacheOperation(method, targetClass);
      if (operation != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Adding cacheable method '" + method.getName() + "' with operation: " + operation);
        }
        this.cache.put(cacheKey, operation);
      }
      else {
        this.cache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
      }
      return operation;
    }
  }

  @Nullable
  private JCacheOperation<?> computeCacheOperation(Method method, @Nullable Class<?> targetClass) {
    // Don't allow non-public methods, as configured.
    if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
      return null;
    }

    // The method may be on an interface, but we need attributes from the target class.
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
