/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.server.reactive;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/22 20:30
 */
class ForwardedHeaderTransformerTests {

  private static final String BASE_URL = "https://example.com/path";

  private final ForwardedHeaderTransformer requestMutator = new ForwardedHeaderTransformer();

  @Test
  void removeOnly() {
    this.requestMutator.setRemoveOnly(true);

    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "for=192.0.2.60;proto=http;by=203.0.113.43");
    headers.add("X-Forwarded-Host", "example.com");
    headers.add("X-Forwarded-Port", "8080");
    headers.add("X-Forwarded-Proto", "http");
    headers.add("X-Forwarded-Prefix", "prefix");
    headers.add("X-Forwarded-Ssl", "on");
    headers.add("X-Forwarded-For", "203.0.113.195");
    ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

    assertForwardedHeadersRemoved(request);
  }

  @Test
  void xForwardedHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Forwarded-Host", "84.198.58.199");
    headers.add("X-Forwarded-Port", "443");
    headers.add("X-Forwarded-Proto", "https");
    headers.add("foo", "bar");
    ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

    assertThat(request.getURI()).isEqualTo(URI.create("https://84.198.58.199/path"));
    assertForwardedHeadersRemoved(request);
  }

  @Test
  void forwardedHeader() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "host=84.198.58.199;proto=https");
    ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

    assertThat(request.getURI()).isEqualTo(URI.create("https://84.198.58.199/path"));
    assertForwardedHeadersRemoved(request);
  }

  @Test
  void xForwardedPrefix() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Forwarded-Prefix", "/prefix");
    ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

    assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/prefix/path"));
    assertThat(request.getPath().value()).isEqualTo("/prefix/path");
    assertForwardedHeadersRemoved(request);
  }

  @Test
    // gh-23305
  void xForwardedPrefixShouldNotLeadToDecodedPath() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Forwarded-Prefix", "/prefix");
    ServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
            .headers(headers)
            .build();

    request = this.requestMutator.apply(request);

    assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/prefix/a%20b?q=a%2Bb"));
    assertThat(request.getPath().value()).isEqualTo("/prefix/a%20b");
    assertForwardedHeadersRemoved(request);
  }

  @Test
  void xForwardedPrefixTrailingSlash() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Forwarded-Prefix", "/prefix////");
    ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

    assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/prefix/path"));
    assertThat(request.getPath().value()).isEqualTo("/prefix/path");
    assertForwardedHeadersRemoved(request);
  }

  @Test
    // SPR-17525
  void shouldNotDoubleEncode() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "host=84.198.58.199;proto=https");

    ServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
            .headers(headers)
            .build();

    request = this.requestMutator.apply(request);

    assertThat(request.getURI()).isEqualTo(URI.create("https://84.198.58.199/a%20b?q=a%2Bb"));
    assertForwardedHeadersRemoved(request);
  }

  @Test
    // gh-30137
  void shouldHandleUnencodedUri() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "host=84.198.58.199;proto=https");
    ServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, URI.create("https://example.com/a?q=1+1=2"))
            .headers(headers)
            .build();

    request = this.requestMutator.apply(request);

    assertThat(request.getURI()).isEqualTo(URI.create("https://84.198.58.199/a?q=1+1=2"));
    assertForwardedHeadersRemoved(request);
  }

  @Test
  void shouldConcatenatePrefixes() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Forwarded-Prefix", "/first,/second");
    ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

    assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/first/second/path"));
    assertThat(request.getPath().value()).isEqualTo("/first/second/path");
    assertForwardedHeadersRemoved(request);
  }

  @Test
  void shouldConcatenatePrefixesWithTrailingSlashes() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Forwarded-Prefix", "/first/,/second//");
    ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

    assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/first/second/path"));
    assertThat(request.getPath().value()).isEqualTo("/first/second/path");
    assertForwardedHeadersRemoved(request);
  }

  @Test
  void forwardedForNotPresent() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "host=84.198.58.199;proto=https");

    InetSocketAddress remoteAddress = new InetSocketAddress("example.client", 47011);

    ServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
            .remoteAddress(remoteAddress)
            .headers(headers)
            .build();

    request = this.requestMutator.apply(request);
    assertThat(request.getRemoteAddress()).isEqualTo(remoteAddress);
  }

  @Test
  void forwardedFor() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "for=\"203.0.113.195:4711\";host=84.198.58.199;proto=https");

    InetSocketAddress remoteAddress = new InetSocketAddress("example.client", 47011);

    ServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
            .remoteAddress(remoteAddress)
            .headers(headers)
            .build();

    request = this.requestMutator.apply(request);
    assertThat(request.getRemoteAddress()).isNotNull();
    assertThat(request.getRemoteAddress().getHostName()).isEqualTo("203.0.113.195");
    assertThat(request.getRemoteAddress().getPort()).isEqualTo(4711);
  }

  @Test
  void xForwardedFor() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("x-forwarded-for", "203.0.113.195, 70.41.3.18, 150.172.238.178");

    ServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
            .headers(headers)
            .build();

    request = this.requestMutator.apply(request);
    assertThat(request.getRemoteAddress()).isNotNull();
    assertThat(request.getRemoteAddress().getHostName()).isEqualTo("203.0.113.195");
  }

  private MockServerHttpRequest getRequest(HttpHeaders headers) {
    return MockServerHttpRequest.get(BASE_URL).headers(headers).build();
  }

  private void assertForwardedHeadersRemoved(ServerHttpRequest request) {
    ForwardedHeaderTransformer.FORWARDED_HEADER_NAMES
            .forEach(name -> assertThat(request.getHeaders().containsKey(name)).isFalse());
  }

}