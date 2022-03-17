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

package cn.taketoday.cache.aspectj;

import cn.taketoday.cache.annotation.AnnotationCacheOperationSource;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.Caching;

/**
 * Concrete AspectJ cache aspect using Framework's @{@link Cacheable} annotation.
 *
 * <p>When using this aspect, you <i>must</i> annotate the implementation class (and/or
 * methods within that class), <i>not</i> the interface (if any) that the class
 * implements. AspectJ follows Java's rule that annotations on interfaces are <i>not</i>
 * inherited.
 *
 * <p>A {@code @Cacheable} annotation on a class specifies the default caching semantics
 * for the execution of any <b>public</b> operation in the class.
 *
 * <p>A {@code @Cacheable} annotation on a method within the class overrides the default
 * caching semantics given by the class annotation (if present). Any method may be
 * annotated (regardless of visibility). Annotating non-public methods directly is the
 * only way to get caching demarcation for the execution of such operations.
 *
 * @author Costin Leau
 * @since 4.0
 */
public aspect AnnotationCacheAspect extends AbstractCacheAspect {

  public AnnotationCacheAspect() {
    super(new AnnotationCacheOperationSource(false));
  }

  /**
   * Matches the execution of any public method in a type with the @{@link Cacheable}
   * annotation, or any subtype of a type with the {@code @Cacheable} annotation.
   */
  private pointcut executionOfAnyPublicMethodInAtCacheableType():
          execution(public * ((@Cacheable *)+).*(..)) && within(@Cacheable *);

  /**
   * Matches the execution of any public method in a type with the @{@link CacheEvict}
   * annotation, or any subtype of a type with the {@code CacheEvict} annotation.
   */
  private pointcut executionOfAnyPublicMethodInAtCacheEvictType():
          execution(public * ((@CacheEvict *)+).*(..)) && within(@CacheEvict *);

  /**
   * Matches the execution of any public method in a type with the @{@link CachePut}
   * annotation, or any subtype of a type with the {@code CachePut} annotation.
   */
  private pointcut executionOfAnyPublicMethodInAtCachePutType():
          execution(public * ((@CachePut *)+).*(..)) && within(@CachePut *);

  /**
   * Matches the execution of any public method in a type with the @{@link Caching}
   * annotation, or any subtype of a type with the {@code Caching} annotation.
   */
  private pointcut executionOfAnyPublicMethodInAtCachingType():
          execution(public * ((@Caching *)+).*(..)) && within(@Caching *);

  /**
   * Matches the execution of any method with the @{@link Cacheable} annotation.
   */
  private pointcut executionOfCacheableMethod():
          execution(@Cacheable * *(..));

  /**
   * Matches the execution of any method with the @{@link CacheEvict} annotation.
   */
  private pointcut executionOfCacheEvictMethod():
          execution(@CacheEvict * *(..));

  /**
   * Matches the execution of any method with the @{@link CachePut} annotation.
   */
  private pointcut executionOfCachePutMethod():
          execution(@CachePut * *(..));

  /**
   * Matches the execution of any method with the @{@link Caching} annotation.
   */
  private pointcut executionOfCachingMethod():
          execution(@Caching * *(..));

  /**
   * Definition of pointcut from super aspect - matched join points will have Spring
   * cache management applied.
   */
  protected pointcut cacheMethodExecution(Object cachedObject):
          (executionOfAnyPublicMethodInAtCacheableType()
                  || executionOfAnyPublicMethodInAtCacheEvictType()
                  || executionOfAnyPublicMethodInAtCachePutType()
                  || executionOfAnyPublicMethodInAtCachingType()
                  || executionOfCacheableMethod()
                  || executionOfCacheEvictMethod()
                  || executionOfCachePutMethod()
                  || executionOfCachingMethod())
                  && this(cachedObject);
}
