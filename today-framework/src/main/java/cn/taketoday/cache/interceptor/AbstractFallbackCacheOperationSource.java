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

package cn.taketoday.cache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.MethodClassKey;

/**
 * Abstract implementation of {@link CacheOperation} that caches attributes
 * for methods and implements a fallback policy: 1. specific target method;
 * 2. target class; 3. declaring method; 4. declaring class/interface.
 *
 * <p>Defaults to using the target class's caching attribute if none is
 * associated with the target method. Any caching attribute associated with
 * the target method completely overrides a class caching attribute.
 * If none found on the target class, the interface that the invoked method
 * has been called through (in case of a JDK proxy) will be checked.
 *
 * <p>This implementation caches attributes by method after they are first
 * used. If it is ever desirable to allow dynamic changing of cacheable
 * attributes (which is very unlikely), caching could be made configurable.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractFallbackCacheOperationSource implements CacheOperationSource {

  /**
   * Canonical value held in cache to indicate no caching attribute was
   * found for this method and we don't need to look again.
   */
  private static final Collection<CacheOperation> NULL_CACHING_ATTRIBUTE = Collections.emptyList();

  /**
   * Logger available to subclasses.
   * <p>As this base class is not marked Serializable, the logger will be recreated
   * after serialization - provided that the concrete subclass is Serializable.
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Cache of CacheOperations, keyed by method on a specific target class.
   * <p>As this base class is not marked Serializable, the cache will be recreated
   * after serialization - provided that the concrete subclass is Serializable.
   */
  private final Map<Object, Collection<CacheOperation>> attributeCache = new ConcurrentHashMap<>(1024);

  /**
   * Determine the caching attribute for this method invocation.
   * <p>Defaults to the class's caching attribute if no method attribute is found.
   *
   * @param method the method for the current invocation (never {@code null})
   * @param targetClass the target class for this invocation (may be {@code null})
   * @return {@link CacheOperation} for this method, or {@code null} if the method
   * is not cacheable
   */
  @Override
  @Nullable
  public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
    if (method.getDeclaringClass() == Object.class) {
      return null;
    }

    Object cacheKey = getCacheKey(method, targetClass);
    Collection<CacheOperation> cached = attributeCache.get(cacheKey);

    if (cached != null) {
      return cached != NULL_CACHING_ATTRIBUTE ? cached : null;
    }
    else {
      Collection<CacheOperation> cacheOps = computeCacheOperations(method, targetClass);
      if (cacheOps != null) {
        if (logger.isTraceEnabled()) {
          logger.trace("Adding cacheable method '{}' with attribute: {}", method.getName(), cacheOps);
        }
        attributeCache.put(cacheKey, cacheOps);
      }
      else {
        attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
      }
      return cacheOps;
    }
  }

  /**
   * Determine a cache key for the given method and target class.
   * <p>Must not produce same key for overloaded methods.
   * Must produce same key for different instances of the same method.
   *
   * @param method the method (never {@code null})
   * @param targetClass the target class (may be {@code null})
   * @return the cache key (never {@code null})
   */
  protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
    return new MethodClassKey(method, targetClass);
  }

  @Nullable
  private Collection<CacheOperation> computeCacheOperations(Method method, @Nullable Class<?> targetClass) {
    // Don't allow non-public methods, as configured.
    if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
      return null;
    }

    // The method may be on an interface, but we need attributes from the target class.
    // If the target class is null, the method will be unchanged.
    Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

    // First try is the method in the target class.
    Collection<CacheOperation> opDef = findCacheOperations(specificMethod);
    if (opDef != null) {
      return opDef;
    }

    // Second try is the caching operation on the target class.
    opDef = findCacheOperations(specificMethod.getDeclaringClass());
    if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
      return opDef;
    }

    if (specificMethod != method) {
      // Fallback is to look at the original method.
      opDef = findCacheOperations(method);
      if (opDef != null) {
        return opDef;
      }
      // Last fallback is the class of the original method.
      opDef = findCacheOperations(method.getDeclaringClass());
      if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
        return opDef;
      }
    }

    return null;
  }

  /**
   * Subclasses need to implement this to return the caching attribute for the
   * given class, if any.
   *
   * @param clazz the class to retrieve the attribute for
   * @return all caching attribute associated with this class, or {@code null} if none
   */
  @Nullable
  protected abstract Collection<CacheOperation> findCacheOperations(Class<?> clazz);

  /**
   * Subclasses need to implement this to return the caching attribute for the
   * given method, if any.
   *
   * @param method the method to retrieve the attribute for
   * @return all caching attribute associated with this method, or {@code null} if none
   */
  @Nullable
  protected abstract Collection<CacheOperation> findCacheOperations(Method method);

  /**
   * Should only public methods be allowed to have caching semantics?
   * <p>The default implementation returns {@code false}.
   */
  protected boolean allowPublicMethodsOnly() {
    return false;
  }

}
