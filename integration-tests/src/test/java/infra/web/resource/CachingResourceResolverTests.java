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

package infra.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for
 * {@link infra.web.resource.CachingResourceResolver}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(GzipSupport.class)
class CachingResourceResolverTests {

  private Cache cache;

  private ResourceResolvingChain chain;

  private List<Resource> locations;

  @BeforeEach
  public void setup() {

    this.cache = new ConcurrentMapCache("resourceCache");

    List<ResourceResolver> resolvers = new ArrayList<>();
    resolvers.add(new CachingResourceResolver(this.cache));
    resolvers.add(new PathResourceResolver());
    this.chain = new DefaultResourceResolvingChain(resolvers);

    this.locations = new ArrayList<>();
    this.locations.add(new ClassPathResource("test/", getClass()));
  }

  @Test
  public void resolveResourceInternal() {
    Resource expected = new ClassPathResource("test/bar.css", getClass());
    Resource actual = this.chain.resolveResource(null, "bar.css", this.locations);

    assertThat(actual).isNotSameAs(expected);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void resolveResourceInternalFromCache() {
    Resource expected = mock(Resource.class);
    this.cache.put(resourceKey("bar.css"), expected);
    Resource actual = this.chain.resolveResource(null, "bar.css", this.locations);

    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void resolveResourceInternalNoMatch() {
    assertThat(this.chain.resolveResource(null, "invalid.css", this.locations)).isNull();
  }

  @Test
  public void resolverUrlPath() {
    String expected = "/foo.css";
    String actual = this.chain.resolveUrlPath(expected, this.locations);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void resolverUrlPathFromCache() {
    String expected = "cached-imaginary.css";
    this.cache.put(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css", expected);
    String actual = this.chain.resolveUrlPath("imaginary.css", this.locations);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void resolverUrlPathNoMatch() {
    assertThat(this.chain.resolveUrlPath("invalid.css", this.locations)).isNull();
  }

  @Test
  public void resolveResourceAcceptEncodingInCacheKey(GzipSupport.GzippedFiles gzippedFiles) throws IOException {

    String file = "bar.css";
    gzippedFiles.create(file);

    // 1. Resolve plain resource

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", file);
    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    Resource expected = this.chain.resolveResource(requestContext, file, this.locations);

    String cacheKey = resourceKey(file);
    assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);

    // 2. Resolve with Accept-Encoding

    request = new HttpMockRequestImpl("GET", file);
    request.addHeader("Accept-Encoding", "gzip ; a=b  , deflate ,  br  ; c=d ");
    requestContext = new MockRequestContext(null, request, null);

    expected = this.chain.resolveResource(requestContext, file, this.locations);

    cacheKey = resourceKey(file + "+encoding=br,gzip");
    assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);

    // 3. Resolve with Accept-Encoding but no matching codings

    request = new HttpMockRequestImpl("GET", file);
    request.addHeader("Accept-Encoding", "deflate");
    requestContext = new MockRequestContext(null, request, null);

    expected = this.chain.resolveResource(requestContext, file, this.locations);

    cacheKey = resourceKey(file);
    assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);
  }

  @Test
  public void resolveResourceNoAcceptEncoding() {
    String file = "bar.css";
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", file);
    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    Resource expected = this.chain.resolveResource(requestContext, file, this.locations);

    String cacheKey = resourceKey(file);
    Object actual = this.cache.get(cacheKey).get();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void resolveResourceMatchingEncoding() {
    Resource resource = mock(Resource.class);
    Resource gzipped = mock(Resource.class);
    this.cache.put(resourceKey("bar.css"), resource);
    this.cache.put(resourceKey("bar.css+encoding=gzip"), gzipped);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "bar.css");
    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    assertThat(this.chain.resolveResource(requestContext, "bar.css", this.locations)).isSameAs(resource);

    request = new HttpMockRequestImpl("GET", "bar.css");
    request.addHeader("Accept-Encoding", "gzip");
    requestContext = new MockRequestContext(null, request, null);

    assertThat(this.chain.resolveResource(requestContext, "bar.css", this.locations)).isSameAs(gzipped);
  }

  @Test
  void constructorWithCacheShouldSetCache() {
    Cache cache = mock(Cache.class);

    CachingResourceResolver resolver = new CachingResourceResolver(cache);

    assertThat(resolver.getCache()).isSameAs(cache);
  }

  @Test
  void constructorWithCacheShouldThrowExceptionWhenCacheIsNull() {
    assertThatThrownBy(() -> new CachingResourceResolver((Cache) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cache is required");
  }

  @Test
  void constructorWithCacheManagerAndCacheNameShouldSetCache() {
    CacheManager cacheManager = mock(CacheManager.class);
    Cache cache = mock(Cache.class);
    String cacheName = "testCache";

    when(cacheManager.getCache(cacheName)).thenReturn(cache);

    CachingResourceResolver resolver = new CachingResourceResolver(cacheManager, cacheName);

    assertThat(resolver.getCache()).isSameAs(cache);
  }

  @Test
  void constructorWithCacheManagerShouldThrowExceptionWhenCacheNotFound() {
    CacheManager cacheManager = mock(CacheManager.class);
    String cacheName = "nonExistentCache";

    when(cacheManager.getCache(cacheName)).thenReturn(null);

    assertThatThrownBy(() -> new CachingResourceResolver(cacheManager, cacheName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cache 'nonExistentCache' not found");
  }

  @Test
  void setContentCodingsShouldUpdateSupportedCodings() {
    CachingResourceResolver resolver = new CachingResourceResolver(mock(Cache.class));
    List<String> customCodings = List.of("deflate", "gzip");

    resolver.setContentCodings(customCodings);

    assertThat(resolver.getContentCodings()).containsExactly("deflate", "gzip");
  }

  @Test
  void setContentCodingsShouldThrowExceptionWhenEmpty() {
    CachingResourceResolver resolver = new CachingResourceResolver(mock(Cache.class));

    assertThatThrownBy(() -> resolver.setContentCodings(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one content coding expected");
  }

  @Test
  void computeKeyShouldReturnKeyWithoutEncodingWhenRequestIsNull() {
    CachingResourceResolver resolver = new CachingResourceResolver(mock(Cache.class));
    String requestPath = "css/style.css";

    String key = resolver.computeKey(null, requestPath);

    assertThat(key).isEqualTo(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath);
  }

  @Test
  void computeKeyShouldReturnKeyWithoutEncodingWhenAcceptEncodingHeaderIsNull() {
    CachingResourceResolver resolver = new CachingResourceResolver(mock(Cache.class));
    RequestContext request = mock(RequestContext.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(request.getHeaders()).thenReturn(headers);
    String requestPath = "css/style.css";

    String key = resolver.computeKey(request, requestPath);

    assertThat(key).isEqualTo(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath);
  }

  @Test
  void computeKeyShouldReturnKeyWithEncodingWhenAcceptEncodingHeaderIsPresent() {
    CachingResourceResolver resolver = new CachingResourceResolver(mock(Cache.class));
    RequestContext request = mock(RequestContext.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept-Encoding", "gzip, br");
    when(request.getHeaders()).thenReturn(headers);
    String requestPath = "css/style.css";

    String key = resolver.computeKey(request, requestPath);

    assertThat(key).isEqualTo(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath + "+encoding=br,gzip");
  }

  @Test
  void computeKeyShouldFilterAndSortSupportedCodingsOnly() {
    CachingResourceResolver resolver = new CachingResourceResolver(mock(Cache.class));
    resolver.setContentCodings(List.of("gzip", "deflate"));
    RequestContext request = mock(RequestContext.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept-Encoding", "br, gzip, identity");
    when(request.getHeaders()).thenReturn(headers);
    String requestPath = "js/script.js";

    String key = resolver.computeKey(request, requestPath);

    assertThat(key).isEqualTo(CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath + "+encoding=gzip");
  }

  @Test
  void getContentCodingKeyShouldReturnNullWhenHeaderIsEmpty() {
    CachingResourceResolver resolver = new CachingResourceResolver(mock(Cache.class));
    RequestContext request = mock(RequestContext.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept-Encoding", "");
    when(request.getHeaders()).thenReturn(headers);

    // Using reflection to test private method
    String key = resolver.getContentCodingKey(request);

    assertThat(key).isNull();
  }

  @Test
  void getContentCodingKeyShouldParseAndFilterCodingWithParameters() {
    CachingResourceResolver resolver = new CachingResourceResolver(mock(Cache.class));
    RequestContext request = mock(RequestContext.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept-Encoding", "gzip;q=0.8, deflate;q=1.0, br");
    when(request.getHeaders()).thenReturn(headers);

    // Using reflection to test private method
    String key = resolver.getContentCodingKey(request);

    assertThat(key).isEqualTo("br,gzip");
  }

  @Test
  void resolveUrlPathInternalShouldReturnCachedValueWhenFound() {
    Cache cache = mock(Cache.class);
    CachingResourceResolver resolver = new CachingResourceResolver(cache);
    List<Resource> locations = List.of();
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    String resourceUrlPath = "css/style.css";
    String cachedPath = "css/style-12345.css";

    when(cache.get(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath, String.class))
            .thenReturn(cachedPath);

    String result = resolver.resolveUrlPathInternal(resourceUrlPath, locations, chain);

    assertThat(result).isEqualTo(cachedPath);
  }

  @Test
  void resolveUrlPathInternalShouldDelegateToChainAndCacheWhenNotInCache() {
    Cache cache = mock(Cache.class);
    CachingResourceResolver resolver = new CachingResourceResolver(cache);
    List<Resource> locations = List.of();
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    String resourceUrlPath = "css/style.css";
    String resolvedPath = "css/style-12345.css";

    when(cache.get(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath, String.class))
            .thenReturn(null);
    when(chain.resolveUrlPath(resourceUrlPath, locations)).thenReturn(resolvedPath);

    String result = resolver.resolveUrlPathInternal(resourceUrlPath, locations, chain);

    assertThat(result).isEqualTo(resolvedPath);
    verify(cache).put(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath, resolvedPath);
  }

  @Test
  void resolveUrlPathInternalShouldReturnNullWhenChainReturnsNull() {
    Cache cache = mock(Cache.class);
    CachingResourceResolver resolver = new CachingResourceResolver(cache);
    List<Resource> locations = List.of();
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    String resourceUrlPath = "nonexistent.css";

    when(cache.get(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath, String.class))
            .thenReturn(null);
    when(chain.resolveUrlPath(resourceUrlPath, locations)).thenReturn(null);

    String result = resolver.resolveUrlPathInternal(resourceUrlPath, locations, chain);

    assertThat(result).isNull();
    verify(cache, never()).put(anyString(), anyString());
  }

  private static String resourceKey(String key) {
    return CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + key;
  }

}
