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

import java.util.Collection;
import java.util.Collections;

import infra.cache.CacheManager;
import infra.cache.interceptor.AbstractCacheResolver;
import infra.cache.interceptor.BasicOperation;
import infra.cache.interceptor.CacheOperationInvocationContext;
import infra.cache.interceptor.CacheResolver;

/**
 * A simple {@link CacheResolver} that resolves the exception cache
 * based on a configurable {@link CacheManager} and the name of the
 * cache: {@link CacheResultOperation#getExceptionCacheName()}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see CacheResultOperation#getExceptionCacheName()
 * @since 4.0
 */
public class SimpleExceptionCacheResolver extends AbstractCacheResolver {

  public SimpleExceptionCacheResolver(CacheManager cacheManager) {
    super(cacheManager);
  }

  @Nullable
  @Override
  protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
    BasicOperation operation = context.getOperation();
    if (!(operation instanceof CacheResultOperation cacheResultOperation)) {
      throw new IllegalStateException("Could not extract exception cache name from " + operation);
    }
    String exceptionCacheName = cacheResultOperation.getExceptionCacheName();
    if (exceptionCacheName != null) {
      return Collections.singleton(exceptionCacheName);
    }
    return null;
  }

}
