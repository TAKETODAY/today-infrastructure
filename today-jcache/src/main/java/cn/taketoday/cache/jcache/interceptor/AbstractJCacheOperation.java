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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheValue;

import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ExceptionTypeFilter;

/**
 * A base {@link JCacheOperation} implementation.
 *
 * @param <A> the annotation type
 * @author Stephane Nicoll
 * @since 4.0
 */
abstract class AbstractJCacheOperation<A extends Annotation> implements JCacheOperation<A> {

  private final CacheMethodDetails<A> methodDetails;

  private final CacheResolver cacheResolver;

  protected final List<CacheParameterDetail> allParameterDetails;

  /**
   * Construct a new {@code AbstractJCacheOperation}.
   *
   * @param methodDetails the {@link CacheMethodDetails} related to the cached method
   * @param cacheResolver the cache resolver to resolve regular caches
   */
  protected AbstractJCacheOperation(CacheMethodDetails<A> methodDetails, CacheResolver cacheResolver) {
    Assert.notNull(methodDetails, "CacheMethodDetails is required");
    Assert.notNull(cacheResolver, "CacheResolver is required");
    this.methodDetails = methodDetails;
    this.cacheResolver = cacheResolver;
    this.allParameterDetails = initializeAllParameterDetails(methodDetails.getMethod());
  }

  private static List<CacheParameterDetail> initializeAllParameterDetails(Method method) {
    int parameterCount = method.getParameterCount();
    List<CacheParameterDetail> result = new ArrayList<>(parameterCount);
    for (int i = 0; i < parameterCount; i++) {
      CacheParameterDetail detail = new CacheParameterDetail(method, i);
      result.add(detail);
    }
    return result;
  }

  @Override
  public Method getMethod() {
    return this.methodDetails.getMethod();
  }

  @Override
  public Set<Annotation> getAnnotations() {
    return this.methodDetails.getAnnotations();
  }

  @Override
  public A getCacheAnnotation() {
    return this.methodDetails.getCacheAnnotation();
  }

  @Override
  public String getCacheName() {
    return this.methodDetails.getCacheName();
  }

  @Override
  public Set<String> getCacheNames() {
    return Collections.singleton(getCacheName());
  }

  @Override
  public CacheResolver getCacheResolver() {
    return this.cacheResolver;
  }

  @Override
  public CacheInvocationParameter[] getAllParameters(Object... values) {
    if (this.allParameterDetails.size() != values.length) {
      throw new IllegalStateException("Values mismatch, operation has " +
              this.allParameterDetails.size() + " parameter(s) but got " + values.length + " value(s)");
    }
    List<CacheInvocationParameter> result = new ArrayList<>();
    for (int i = 0; i < this.allParameterDetails.size(); i++) {
      result.add(this.allParameterDetails.get(i).toCacheInvocationParameter(values[i]));
    }
    return result.toArray(new CacheInvocationParameter[0]);
  }

  /**
   * Return the {@link ExceptionTypeFilter} to use to filter exceptions thrown while
   * invoking the method.
   *
   * @see #createExceptionTypeFilter
   */
  public abstract ExceptionTypeFilter getExceptionTypeFilter();

  /**
   * Convenience method for subclasses to create a specific {@code ExceptionTypeFilter}.
   *
   * @see #getExceptionTypeFilter()
   */
  protected ExceptionTypeFilter createExceptionTypeFilter(
          Class<? extends Throwable>[] includes, Class<? extends Throwable>[] excludes) {

    return new ExceptionTypeFilter(Arrays.asList(includes), Arrays.asList(excludes), true);
  }

  @Override
  public String toString() {
    return getOperationDescription().append(']').toString();
  }

  /**
   * Return an identifying description for this caching operation.
   * <p>Available to subclasses, for inclusion in their {@code toString()} result.
   */
  protected StringBuilder getOperationDescription() {
    StringBuilder result = new StringBuilder();
    result.append(getClass().getSimpleName());
    result.append('[');
    result.append(this.methodDetails);
    return result;
  }

  /**
   * Details for a single cache parameter.
   */
  protected static class CacheParameterDetail {

    private final Class<?> rawType;

    private final Set<Annotation> annotations;

    private final int parameterPosition;

    private final boolean isKey;

    private final boolean isValue;

    public CacheParameterDetail(Method method, int parameterPosition) {
      this.rawType = method.getParameterTypes()[parameterPosition];
      this.annotations = new LinkedHashSet<>();
      boolean foundKeyAnnotation = false;
      boolean foundValueAnnotation = false;
      for (Annotation annotation : method.getParameterAnnotations()[parameterPosition]) {
        this.annotations.add(annotation);
        if (CacheKey.class.isAssignableFrom(annotation.annotationType())) {
          foundKeyAnnotation = true;
        }
        if (CacheValue.class.isAssignableFrom(annotation.annotationType())) {
          foundValueAnnotation = true;
        }
      }
      this.parameterPosition = parameterPosition;
      this.isKey = foundKeyAnnotation;
      this.isValue = foundValueAnnotation;
    }

    public int getParameterPosition() {
      return this.parameterPosition;
    }

    protected boolean isKey() {
      return this.isKey;
    }

    protected boolean isValue() {
      return this.isValue;
    }

    public CacheInvocationParameter toCacheInvocationParameter(Object value) {
      return new CacheInvocationParameterImpl(this, value);
    }
  }

  /**
   * A single cache invocation parameter.
   */
  protected static class CacheInvocationParameterImpl implements CacheInvocationParameter {

    private final CacheParameterDetail detail;

    private final Object value;

    public CacheInvocationParameterImpl(CacheParameterDetail detail, Object value) {
      this.detail = detail;
      this.value = value;
    }

    @Override
    public Class<?> getRawType() {
      return this.detail.rawType;
    }

    @Override
    public Object getValue() {
      return this.value;
    }

    @Override
    public Set<Annotation> getAnnotations() {
      return this.detail.annotations;
    }

    @Override
    public int getParameterPosition() {
      return this.detail.parameterPosition;
    }
  }

}
