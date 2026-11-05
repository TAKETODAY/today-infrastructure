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

package infra.web.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import infra.http.server.RequestPath;
import infra.web.HttpContextHolder;
import infra.web.filter.ForwardedHeaderFilter;
import infra.web.mock.MockFilterChain;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link HttpContextUriComponentsBuilder}.
 *
 * @author Rossen Stoyanchev
 */
class HttpContextUriComponentsBuilderTests {

  private final MockRequest request = new MockRequest();

  @AfterEach
  void cleanup() {
    HttpContextHolder.cleanup();
  }

  @Test
  void fromRequest() {
    this.request.setRequestURI("/app/data/foo%20bar");
    this.request.setQueryString("foo=123&q=a%2Bb");
    assertThat(HttpContextUriComponentsBuilder.fromRequest(context()).build().toUriString())
            .isEqualTo("http://localhost/app/data/foo%20bar?foo=123&q=a%2Bb");
  }

  @ParameterizedTest
  @CsvSource({ "http,80,http://localhost/app", "https,443,https://localhost/app",
          "http,8080,http://localhost:8080/app", "https,9043,https://localhost:9043/app",
          "http,-1,http://localhost/app" })
  void requestPort(String scheme, int port, String expected) {
    this.request.setScheme(scheme);
    this.request.setServerPort(port);
    this.request.setRequestURI("/app");
    assertThat(HttpContextUriComponentsBuilder.fromRequest(context()).build().toUriString()).isEqualTo(expected);
  }

  @Test
  void requestPortWithUppercaseScheme() {
    this.request.setScheme("HTTPS");
    this.request.setServerPort(443);
    this.request.setRequestURI("/app");
    assertThat(HttpContextUriComponentsBuilder.fromRequest(context()).build().toUriString())
            .isEqualTo("HTTPS://localhost/app");
  }

  @Test
  void fromRequestUri() {
    this.request.setRequestURI("/app/data");
    this.request.setQueryString("foo=123");
    assertThat(HttpContextUriComponentsBuilder.fromRequestUri(context()).build().toUriString())
            .isEqualTo("http://localhost/app/data");
  }

  @Test
  void fromContextPath() {
    this.request.setRequestURI("/app/data");
    this.request.setQueryString("foo=123");
    assertThat(HttpContextUriComponentsBuilder.fromContextPath(context()).build().toUriString())
            .isEqualTo("http://localhost/app");
  }

  @Test
  void ignoresUnprocessedForwardedHeaders() {
    this.request.setRequestURI("/app/data");
    this.request.addHeader("Forwarded", "proto=https;host=example.org");
    assertThat(HttpContextUriComponentsBuilder.fromRequest(context()).build().toUriString())
            .isEqualTo("http://localhost/app/data");
  }

  @ParameterizedTest
  @CsvSource({ "/edge,/edge", "/edge/,/edge", "/,''" })
  void usesAdaptedRequest(String prefix, String expectedPrefix) throws Exception {
    this.request.setRequestURI("/data/a%2Fb");
    this.request.setQueryString("q=a%2Bb");
    this.request.addHeader("X-Forwarded-Proto", "https");
    this.request.addHeader("X-Forwarded-Host", "example.org");
    this.request.addHeader("X-Forwarded-Port", "443");
    this.request.addHeader("X-Forwarded-Prefix", prefix);
    ForwardedHeaderFilter filter = new ForwardedHeaderFilter(false);
    filter.setUseForwardedPrefix(true);
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(new MockHttpContext(this.request), chain);

    assertThat(HttpContextUriComponentsBuilder.fromRequest(chain.getContext()).build().toUriString())
            .isEqualTo("https://example.org" + expectedPrefix + "/data/a%2Fb?q=a%2Bb");
    assertThat(HttpContextUriComponentsBuilder.fromContextPath(chain.getContext()).build().toUriString())
            .isEqualTo("https://example.org" + expectedPrefix);
  }

  @Test
  void currentRequestFactories() {
    this.request.setRequestURI("/app/data");
    this.request.setQueryString("foo=123");
    HttpContextHolder.set(context());

    assertThat(HttpContextUriComponentsBuilder.fromCurrentRequest().build().toUriString())
            .isEqualTo("http://localhost/app/data?foo=123");
    assertThat(HttpContextUriComponentsBuilder.fromCurrentRequestUri().build().toUriString())
            .isEqualTo("http://localhost/app/data");
    assertThat(HttpContextUriComponentsBuilder.fromCurrentContextPath().build().toUriString())
            .isEqualTo("http://localhost/app");
  }

  @Test
  void currentRequestRequired() {
    HttpContextHolder.cleanup();
    assertThatIllegalStateException().isThrownBy(HttpContextUriComponentsBuilder::fromCurrentRequest);
    assertThatIllegalStateException().isThrownBy(HttpContextUriComponentsBuilder::fromCurrentRequestUri);
    assertThatIllegalStateException().isThrownBy(HttpContextUriComponentsBuilder::fromCurrentContextPath);
  }

  @Test
  void pathExtension() {
    this.request.setRequestURI("/rest/books/6.json");
    HttpContextUriComponentsBuilder builder = HttpContextUriComponentsBuilder.fromRequestUri(context());
    String extension = builder.removePathExtension();
    assertThat(extension).isEqualTo("json");
    assertThat(builder.path("/pages/1.{ext}").buildAndExpand(extension).toUriString())
            .isEqualTo("http://localhost/rest/books/6/pages/1.json");
    assertThat(builder.removePathExtension()).isNull();
  }

  @Test
  void pathExtensionNone() {
    this.request.setRequestURI("/rest/books/6");
    assertThat(HttpContextUriComponentsBuilder.fromRequestUri(context()).removePathExtension()).isNull();
  }

  @Test
  void clonePreservesOriginalPathAndIsIndependent() {
    this.request.setRequestURI("/app/data.json");
    this.request.setQueryString("foo=123");
    HttpContextUriComponentsBuilder builder = HttpContextUriComponentsBuilder.fromRequest(context());
    HttpContextUriComponentsBuilder copy = builder.cloneBuilder();
    assertThat(copy.removePathExtension()).isEqualTo("json");
    copy.queryParam("bar", "456");
    assertThat(copy.build().toUriString()).isEqualTo("http://localhost/app/data?foo=123&bar=456");
    assertThat(builder.build().toUriString()).isEqualTo("http://localhost/app/data.json?foo=123");
    assertThat(builder.removePathExtension()).isEqualTo("json");
    assertThat(builder.clone()).isInstanceOf(HttpContextUriComponentsBuilder.class);
  }

  private MockHttpContext context() {
    return new MockHttpContext(this.request) {
      @Override
      public RequestPath getRequestPath() {
        return RequestPath.parse(getRequestURI(), getRequestURI().startsWith("/app/") ? "/app" : null);
      }
    };
  }

}
