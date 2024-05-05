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

package cn.taketoday.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.resource.GzipSupport.GzippedFiles;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EncodedResourceResolver}.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 */
@ExtendWith(GzipSupport.class)
public class EncodedResourceResolverTests {

  private ResourceResolvingChain resolver;

  private List<Resource> locations;

  private Cache cache;

  @BeforeEach
  public void setup() {
    this.cache = new ConcurrentMapCache("resourceCache");

    VersionResourceResolver versionResolver = new VersionResourceResolver();
    versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));

    List<ResourceResolver> resolvers = new ArrayList<>();
    resolvers.add(new CachingResourceResolver(this.cache));
    resolvers.add(new EncodedResourceResolver());
    resolvers.add(versionResolver);
    resolvers.add(new PathResourceResolver());
    this.resolver = new DefaultResourceResolvingChain(resolvers);

    this.locations = new ArrayList<>();
    this.locations.add(new ClassPathResource("test/", getClass()));
    this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
  }

  @Test
  public void resolveGzipped(GzippedFiles gzippedFiles) {
    String file = "js/foo.js";
    gzippedFiles.create(file);
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Accept-Encoding", "gzip");

    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    Resource actual = this.resolver.resolveResource(requestContext, file, this.locations);

    assertThat(actual).isEqualTo(getResource(file + ".gz"));
    assertThat(actual.getName()).isEqualTo(getResource(file).getName());

    boolean condition = actual instanceof HttpResource;
    assertThat(condition).isTrue();
    HttpHeaders headers = ((HttpResource) actual).getResponseHeaders();
    assertThat(headers.getFirst(HttpHeaders.CONTENT_ENCODING)).isEqualTo("gzip");
    assertThat(headers.getFirst(HttpHeaders.VARY)).isEqualTo("Accept-Encoding");
  }

  @Test
  public void resolveGzippedWithVersion(GzippedFiles gzippedFiles) {
    gzippedFiles.create("foo.css");
    String file = "foo-e36d2e05253c6c7085a91522ce43a0b4.css";
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Accept-Encoding", "gzip");
    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    Resource resolved = this.resolver.resolveResource(requestContext, file, this.locations);

    assertThat(resolved).isEqualTo(getResource("foo.css.gz"));
    assertThat(resolved.getName()).isEqualTo(getResource("foo.css").getName());
    boolean condition = resolved instanceof HttpResource;
    assertThat(condition).isTrue();
  }

  @Test
  public void resolveFromCacheWithEncodingVariants(GzippedFiles gzippedFiles) {
    // 1. Resolve, and cache .gz variant
    String file = "js/foo.js";
    gzippedFiles.create(file);
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/js/foo.js");
    request.addHeader("Accept-Encoding", "gzip");
    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    Resource resolved = this.resolver.resolveResource(requestContext, file, this.locations);

    assertThat(resolved).isEqualTo(getResource(file + ".gz"));
    assertThat(resolved.getName()).isEqualTo(getResource(file).getName());
    boolean condition = resolved instanceof HttpResource;
    assertThat(condition).isTrue();

    // 2. Resolve unencoded resource
    request = new HttpMockRequestImpl("GET", "/js/foo.js");
    requestContext = new MockRequestContext(null, request, null);

    resolved = this.resolver.resolveResource(requestContext, file, this.locations);

    assertThat(resolved.toString()).isEqualTo(getResource(file).toString());
    assertThat(resolved.getName()).isEqualTo(getResource(file).getName());
    boolean condition1 = resolved instanceof HttpResource;
    assertThat(condition1).isFalse();
  }

  @Test  // SPR-13149
  public void resolveWithNullRequest() {
    String file = "js/foo.js";
    Resource resolved = this.resolver.resolveResource(null, file, this.locations);

    assertThat(resolved.toString()).isEqualTo(getResource(file).toString());
    assertThat(resolved.getName()).isEqualTo(getResource(file).getName());
  }

  private Resource getResource(String filePath) {
    return new ClassPathResource("test/" + filePath, getClass());
  }

}
