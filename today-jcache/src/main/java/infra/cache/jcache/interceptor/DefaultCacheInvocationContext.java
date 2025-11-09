/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.cache.jcache.interceptor;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheInvocationParameter;

import infra.cache.interceptor.CacheOperationInvocationContext;

/**
 * The default {@link CacheOperationInvocationContext} implementation used
 * by all interceptors. Also implements {@link CacheInvocationContext} to
 * act as a proper bridge when calling JSR-107 {@link javax.cache.annotation.CacheResolver}
 *
 * @param <A> the annotation type
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
class DefaultCacheInvocationContext<A extends Annotation>
        implements CacheInvocationContext<A>, CacheOperationInvocationContext<JCacheOperation<A>> {

  private final JCacheOperation<A> operation;

  private final Object target;

  private final @Nullable Object[] args;

  private final CacheInvocationParameter[] allParameters;

  public DefaultCacheInvocationContext(JCacheOperation<A> operation, Object target, @Nullable Object[] args) {
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
  public @Nullable Object[] getArgs() {
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
    return "CacheInvocationContext{operation=%s, target=%s, args=%s, allParameters=%s}"
            .formatted(this.operation, this.target, Arrays.toString(this.args), Arrays.toString(this.allParameters));
  }

}
