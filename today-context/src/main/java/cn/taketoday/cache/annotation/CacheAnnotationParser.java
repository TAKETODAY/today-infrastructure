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

package cn.taketoday.cache.annotation;

import java.lang.reflect.Method;
import java.util.Collection;

import cn.taketoday.cache.interceptor.CacheOperation;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for parsing known caching annotation types.
 * {@link AnnotationCacheOperationSource} delegates to such parsers
 * for supporting specific annotation types such as Framework's own
 * {@link Cacheable}, {@link CachePut} and{@link CacheEvict}.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see AnnotationCacheOperationSource
 * @see DefaultCacheAnnotationParser
 * @since 4.0
 */
public interface CacheAnnotationParser {

  /**
   * Determine whether the given class is a candidate for cache operations
   * in the annotation format of this {@code CacheAnnotationParser}.
   * <p>If this method returns {@code false}, the methods on the given class
   * will not get traversed for {@code #parseCacheAnnotations} introspection.
   * Returning {@code false} is therefore an optimization for non-affected
   * classes, whereas {@code true} simply means that the class needs to get
   * fully introspected for each method on the given class individually.
   *
   * @param targetClass the class to introspect
   * @return {@code false} if the class is known to have no cache operation
   * annotations at class or method level; {@code true} otherwise. The default
   * implementation returns {@code true}, leading to regular introspection.
   * @since 4.0
   */
  default boolean isCandidateClass(Class<?> targetClass) {
    return true;
  }

  /**
   * Parse the cache definition for the given class,
   * based on an annotation type understood by this parser.
   * <p>This essentially parses a known cache annotation into Framework's metadata
   * attribute class. Returns {@code null} if the class is not cacheable.
   *
   * @param type the annotated class
   * @return the configured caching operation, or {@code null} if none found
   * @see AnnotationCacheOperationSource#findCacheOperations(Class)
   */
  @Nullable
  Collection<CacheOperation> parseCacheAnnotations(Class<?> type);

  /**
   * Parse the cache definition for the given method,
   * based on an annotation type understood by this parser.
   * <p>This essentially parses a known cache annotation into Framework's metadata
   * attribute class. Returns {@code null} if the method is not cacheable.
   *
   * @param method the annotated method
   * @return the configured caching operation, or {@code null} if none found
   * @see AnnotationCacheOperationSource#findCacheOperations(Method)
   */
  @Nullable
  Collection<CacheOperation> parseCacheAnnotations(Method method);

}
