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
import java.util.Set;

import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheInvocationParameter;

import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;

/**
 * The default {@link CacheOperationInvocationContext} implementation used
 * by all interceptors. Also implements {@link CacheInvocationContext} to
 * act as a proper bridge when calling JSR-107 {@link javax.cache.annotation.CacheResolver}
 *
 * @param <A> the annotation type
 * @author Stephane Nicoll
 * @since 4.0
 */
class DefaultCacheInvocationContext<A extends Annotation>
        implements CacheInvocationContext<A>, CacheOperationInvocationContext<JCacheOperation<A>> {

  private final JCacheOperation<A> operation;

  private final Object target;

  private final Object[] args;

  private final CacheInvocationParameter[] allParameters;

  public DefaultCacheInvocationContext(JCacheOperation<A> operation, Object target, Object[] args) {
    this.operation = operation;
    this.target = target;
    this.args = args;
    this.allParameters = operation.getAllParameters(args);
  }

  @Override
  public JCacheOperation<A> getOperation() {
    return this.operation;
  }

  @Override
  public Method getMethod() {
    return this.operation.getMethod();
  }

  @Override
  public Object[] getArgs() {
    return this.args.clone();
  }

  @Override
  public Set<Annotation> getAnnotations() {
    return this.operation.getAnnotations();
  }

  @Override
  public A getCacheAnnotation() {
    return this.operation.getCacheAnnotation();
  }

  @Override
  public String getCacheName() {
    return this.operation.getCacheName();
  }

  @Override
  public Object getTarget() {
    return this.target;
  }

  @Override
  public CacheInvocationParameter[] getAllParameters() {
    return this.allParameters.clone();
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    throw new IllegalArgumentException("Cannot unwrap to " + cls);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CacheInvocationContext{");
    sb.append("operation=").append(this.operation);
    sb.append(", target=").append(this.target);
    sb.append(", args=").append(Arrays.toString(this.args));
    sb.append(", allParameters=").append(Arrays.toString(this.allParameters));
    sb.append('}');
    return sb.toString();
  }

}
