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

package infra.cache.jcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.List;

import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.cache.interceptor.SimpleCacheResolver;
import infra.cache.interceptor.SimpleKeyGenerator;
import infra.cache.jcache.interceptor.SimpleExceptionCacheResolver;
import infra.cache.support.SimpleCacheManager;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public abstract class AbstractJCacheTests {

  protected String cacheName;

  @BeforeEach
  void trackCacheName(TestInfo testInfo) {
    this.cacheName = testInfo.getTestMethod().get().getName();
  }

  protected final CacheManager cacheManager = createSimpleCacheManager("default", "simpleCache");

  protected final CacheResolver defaultCacheResolver = new SimpleCacheResolver(cacheManager);

  protected final CacheResolver defaultExceptionCacheResolver = new SimpleExceptionCacheResolver(cacheManager);

  protected final KeyGenerator defaultKeyGenerator = new SimpleKeyGenerator();

  protected static CacheManager createSimpleCacheManager(String... cacheNames) {
    SimpleCacheManager result = new SimpleCacheManager();
    List<Cache> caches = new ArrayList<>();
    for (String cacheName : cacheNames) {
      caches.add(new ConcurrentMapCache(cacheName));
    }
    result.setCaches(caches);
    result.afterPropertiesSet();
    return result;
  }

}
