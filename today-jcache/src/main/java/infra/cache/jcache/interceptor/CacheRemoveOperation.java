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

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemove;

import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.util.ExceptionTypeFilter;

/**
 * The {@link JCacheOperation} implementation for a {@link CacheRemove} operation.
 *
 * @author Stephane Nicoll
 * @see CacheRemove
 * @since 4.0
 */
class CacheRemoveOperation extends AbstractJCacheKeyOperation<CacheRemove> {

  private final ExceptionTypeFilter exceptionTypeFilter;

  public CacheRemoveOperation(
          CacheMethodDetails<CacheRemove> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator) {

    super(methodDetails, cacheResolver, keyGenerator);
    CacheRemove ann = methodDetails.getCacheAnnotation();
    this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
  }

  @Override
  public ExceptionTypeFilter getExceptionTypeFilter() {
    return this.exceptionTypeFilter;
  }

  /**
   * Specify if the cache entry should be removed before invoking the method.
   * <p>By default, the cache entry is removed after the method invocation.
   *
   * @see javax.cache.annotation.CacheRemove#afterInvocation()
   */
  public boolean isEarlyRemove() {
    return !getCacheAnnotation().afterInvocation();
  }

}
