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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.cache.annotation.CacheMethodDetails;

/**
 * The default {@link CacheMethodDetails} implementation.
 *
 * @param <A> the annotation type
 * @author Stephane Nicoll
 * @since 4.0
 */
class DefaultCacheMethodDetails<A extends Annotation> implements CacheMethodDetails<A> {

  private final Method method;

  private final Set<Annotation> annotations;

  private final A cacheAnnotation;

  private final String cacheName;

  public DefaultCacheMethodDetails(Method method, A cacheAnnotation, String cacheName) {
    this.method = method;
    this.annotations = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(method.getAnnotations())));
    this.cacheAnnotation = cacheAnnotation;
    this.cacheName = cacheName;
  }

  @Override
  public Method getMethod() {
    return this.method;
  }

  @Override
  public Set<Annotation> getAnnotations() {
    return this.annotations;
  }

  @Override
  public A getCacheAnnotation() {
    return this.cacheAnnotation;
  }

  @Override
  public String getCacheName() {
    return this.cacheName;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CacheMethodDetails[");
    sb.append("method=").append(this.method);
    sb.append(", cacheAnnotation=").append(this.cacheAnnotation);
    sb.append(", cacheName='").append(this.cacheName).append('\'');
    sb.append(']');
    return sb.toString();
  }

}
