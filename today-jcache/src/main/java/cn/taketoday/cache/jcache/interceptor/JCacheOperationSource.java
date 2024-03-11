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

package cn.taketoday.cache.jcache.interceptor;

import java.lang.reflect.Method;

import cn.taketoday.cache.interceptor.CacheOperationSource;
import cn.taketoday.lang.Nullable;

/**
 * Interface used by {@link JCacheInterceptor}. Implementations know how to source
 * cache operation attributes from standard JSR-107 annotations.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CacheOperationSource
 * @since 4.0
 */
public interface JCacheOperationSource {

  /**
   * Determine whether the given class is a candidate for cache operations
   * in the metadata format of this {@code JCacheOperationSource}.
   * <p>If this method returns {@code false}, the methods on the given class
   * will not get traversed for {@link #getCacheOperation} introspection.
   * Returning {@code false} is therefore an optimization for non-affected
   * classes, whereas {@code true} simply means that the class needs to get
   * fully introspected for each method on the given class individually.
   *
   * @param targetClass the class to introspect
   * @return {@code false} if the class is known to have no cache operation
   * metadata at class or method level; {@code true} otherwise. The default
   * implementation returns {@code true}, leading to regular introspection.
   * @see #hasCacheOperation
   */
  default boolean isCandidateClass(Class<?> targetClass) {
    return true;
  }

  /**
   * Determine whether there is a JSR-107 cache operation for the given method.
   *
   * @param method the method to introspect
   * @param targetClass the target class (can be {@code null}, in which case
   * the declaring class of the method must be used)
   * @see #getCacheOperation
   */
  default boolean hasCacheOperation(Method method, @Nullable Class<?> targetClass) {
    return getCacheOperation(method, targetClass) != null;
  }

  /**
   * Return the cache operations for this method, or {@code null}
   * if the method contains no <em>JSR-107</em> related metadata.
   *
   * @param method the method to introspect
   * @param targetClass the target class (can be {@code null}, in which case
   * the declaring class of the method must be used)
   * @return the cache operation for this method, or {@code null} if none found
   */
  @Nullable
  JCacheOperation<?> getCacheOperation(Method method, @Nullable Class<?> targetClass);

}
