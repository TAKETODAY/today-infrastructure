/*
 * Copyright 2017 - 2024 the original author or authors.
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
import infra.cache.concurrent.ConcurrentMapCache;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for
 * {@link infra.web.resource.CachingResourceResolver}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(GzipSupport.class)
public class CachingResourceResolverTests {

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

  private static String resourceKey(String key) {
    return CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + key;
  }

}
