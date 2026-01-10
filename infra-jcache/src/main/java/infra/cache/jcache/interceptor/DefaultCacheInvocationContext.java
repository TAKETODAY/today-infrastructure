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
