/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.core.io.Resource;
import infra.web.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 12:50
 */
class CachingResourceTransformerTests {

  @Test
  void constructorWithCacheShouldSetCache() {
    Cache cache = mock(Cache.class);

    CachingResourceTransformer transformer = new CachingResourceTransformer(cache);

    assertThat(transformer.getCache()).isSameAs(cache);
  }

  @Test
  void constructorWithCacheShouldThrowExceptionWhenCacheIsNull() {
    assertThatThrownBy(() -> new CachingResourceTransformer((Cache) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cache is required");
  }

  @Test
  void constructorWithCacheManagerAndCacheNameShouldSetCache() {
    CacheManager cacheManager = mock(CacheManager.class);
    Cache cache = mock(Cache.class);
    String cacheName = "testCache";

    when(cacheManager.getCache(cacheName)).thenReturn(cache);

    CachingResourceTransformer transformer = new CachingResourceTransformer(cacheManager, cacheName);

    assertThat(transformer.getCache()).isSameAs(cache);
  }

  @Test
  void constructorWithCacheManagerShouldThrowExceptionWhenCacheNotFound() {
    CacheManager cacheManager = mock(CacheManager.class);
    String cacheName = "nonExistentCache";

    when(cacheManager.getCache(cacheName)).thenReturn(null);

    assertThatThrownBy(() -> new CachingResourceTransformer(cacheManager, cacheName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cache 'nonExistentCache' not found");
  }

  @Test
  void transformShouldReturnCachedResourceWhenFound() throws IOException {
    Cache cache = mock(Cache.class);
    RequestContext request = mock(RequestContext.class);
    Resource resource = mock(Resource.class);
    Resource cachedResource = mock(Resource.class);

    when(cache.get(resource, Resource.class)).thenReturn(cachedResource);

    CachingResourceTransformer transformer = new CachingResourceTransformer(cache);
    Resource result = transformer.transform(request, resource, mock(ResourceTransformerChain.class));

    assertThat(result).isSameAs(cachedResource);
  }

  @Test
  void transformShouldDelegateToChainAndCacheResultWhenNotInCache() throws IOException {
    Cache cache = mock(Cache.class);
    RequestContext request = mock(RequestContext.class);
    Resource resource = mock(Resource.class);
    Resource transformedResource = mock(Resource.class);
    ResourceTransformerChain transformerChain = mock(ResourceTransformerChain.class);

    when(cache.get(resource, Resource.class)).thenReturn(null);
    when(transformerChain.transform(request, resource)).thenReturn(transformedResource);

    CachingResourceTransformer transformer = new CachingResourceTransformer(cache);
    Resource result = transformer.transform(request, resource, transformerChain);

    assertThat(result).isSameAs(transformedResource);
    verify(cache).put(resource, transformedResource);
  }

  @Test
  void transformShouldHandleNullCachedResource() throws IOException {
    Cache cache = mock(Cache.class);
    RequestContext request = mock(RequestContext.class);
    Resource resource = mock(Resource.class);
    Resource transformedResource = mock(Resource.class);
    ResourceTransformerChain transformerChain = mock(ResourceTransformerChain.class);

    when(cache.get(resource, Resource.class)).thenReturn(null);
    when(transformerChain.transform(request, resource)).thenReturn(transformedResource);

    CachingResourceTransformer transformer = new CachingResourceTransformer(cache);
    Resource result = transformer.transform(request, resource, transformerChain);

    assertThat(result).isSameAs(transformedResource);
  }

}