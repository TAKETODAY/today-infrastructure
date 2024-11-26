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

package infra.web.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import infra.http.AbstractHttpRequest;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.server.MockServerHttpRequest;
import infra.mock.web.HttpMockRequestImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/15 23:23
 */
class ForwardedHeaderUtilsTests {

  @Test
  void fromHttpRequest() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.setRequestURI("/path");
    request.setQueryString("a=1");

    MockServerHttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();
    assertThat(result.getScheme()).isEqualTo("http");
    assertThat(result.getHost()).isEqualTo("localhost");
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.getPath()).isEqualTo("/path");
    assertThat(result.getQuery()).isEqualTo("a=1");
  }

  @ParameterizedTest // gh-17368, gh-27097
  @ValueSource(strings = { "https", "wss" })
  void fromHttpRequestResetsPort443(String protocol) {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("X-Forwarded-Proto", protocol);
    request.addHeader("X-Forwarded-Host", "84.198.58.199");
    request.addHeader("X-Forwarded-Port", 443);
    request.setScheme("http");
    request.setServerName("example.com");
    request.setServerPort(80);
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo(protocol);
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
  }

  @ParameterizedTest // gh-27097
  @ValueSource(strings = { "http", "ws" })
  void fromHttpRequestResetsPort80(String protocol) {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("X-Forwarded-Proto", protocol);
    request.addHeader("X-Forwarded-Host", "84.198.58.199");
    request.addHeader("X-Forwarded-Port", 80);
    request.setScheme("http");
    request.setServerName("example.com");
    request.setServerPort(80);
    request.setRequestURI("/path");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo(protocol);
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.getPath()).isEqualTo("/path");
  }

  @Test
    // SPR-14761
  void fromHttpRequestWithForwardedIPv4Host() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("https");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("Forwarded", "host=192.168.0.1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("https://192.168.0.1/mvc-showcase");
  }

  @Test
    // SPR-14761
  void fromHttpRequestWithForwardedIPv6() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("Forwarded", "host=[1abc:2abc:3abc::5ABC:6abc]");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("http://[1abc:2abc:3abc::5ABC:6abc]/mvc-showcase");
  }

  @Test
    // SPR-14761
  void fromHttpRequestWithForwardedIPv6Host() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("X-Forwarded-Host", "[1abc:2abc:3abc::5ABC:6abc]");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("http://[1abc:2abc:3abc::5ABC:6abc]/mvc-showcase");
  }

  @Test
    // SPR-14761
  void fromHttpRequestWithForwardedIPv6HostAndPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("X-Forwarded-Host", "[1abc:2abc:3abc::5ABC:6abc]:8080");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("http://[1abc:2abc:3abc::5ABC:6abc]:8080/mvc-showcase");
  }

  @Test
  void fromHttpRequestWithForwardedInvalidIPv6Address() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("X-Forwarded-Host", "2a02:918:175:ab60:45ee:c12c:dac1:808b");

    HttpRequest httpRequest = new MockServerHttpRequest(request);

    assertThatThrownBy(() ->
            ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build())
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fromHttpRequestWithForwardedHost() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("https");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("X-Forwarded-Host", "anotherHost");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("https://anotherHost/mvc-showcase");
  }

  @Test
    // SPR-10701
  void fromHttpRequestWithForwardedHostIncludingPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("X-Forwarded-Host", "webtest.foo.bar.com:443");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getHost()).isEqualTo("webtest.foo.bar.com");
    assertThat(result.getPort()).isEqualTo(443);
  }

  @Test
    // SPR-11140
  void fromHttpRequestWithForwardedHostMultiValuedHeader() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(-1);
    request.addHeader("X-Forwarded-Host", "a.example.org, b.example.org, c.example.org");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getHost()).isEqualTo("a.example.org");
    assertThat(result.getPort()).isEqualTo(-1);
  }

  @Test
    // SPR-11855
  void fromHttpRequestWithForwardedHostAndPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(8080);
    request.addHeader("X-Forwarded-Host", "foobarhost");
    request.addHeader("X-Forwarded-Port", "9090");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getHost()).isEqualTo("foobarhost");
    assertThat(result.getPort()).isEqualTo(9090);
  }

  @Test
    // SPR-11872
  void fromHttpRequestWithForwardedHostWithDefaultPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(10080);
    request.addHeader("X-Forwarded-Host", "example.org");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getHost()).isEqualTo("example.org");
    assertThat(result.getPort()).isEqualTo(-1);
  }

  @Test
    // SPR-16262
  void fromHttpRequestWithForwardedProtoWithDefaultPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("example.org");
    request.setServerPort(10080);
    request.addHeader("X-Forwarded-Proto", "https");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("example.org");
    assertThat(result.getPort()).isEqualTo(-1);
  }

  @Test
    // SPR-16863
  void fromHttpRequestWithForwardedSsl() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("example.org");
    request.setServerPort(10080);
    request.addHeader("X-Forwarded-Ssl", "on");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("example.org");
    assertThat(result.getPort()).isEqualTo(-1);
  }

  @Test
  void fromHttpRequestWithForwardedHostWithForwardedScheme() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(10080);
    request.addHeader("X-Forwarded-Host", "example.org");
    request.addHeader("X-Forwarded-Proto", "https");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getHost()).isEqualTo("example.org");
    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getPort()).isEqualTo(-1);
  }

  @Test
    // SPR-12771
  void fromHttpRequestWithForwardedProtoAndDefaultPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(80);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("X-Forwarded-Proto", "https");
    request.addHeader("X-Forwarded-Host", "84.198.58.199");
    request.addHeader("X-Forwarded-Port", "443");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("https://84.198.58.199/mvc-showcase");
  }

  @Test
    // SPR-12813
  void fromHttpRequestWithForwardedPortMultiValueHeader() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(9090);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("X-Forwarded-Host", "a.example.org");
    request.addHeader("X-Forwarded-Port", "80,52022");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("http://a.example.org/mvc-showcase");
  }

  @Test
    // SPR-12816
  void fromHttpRequestWithForwardedProtoMultiValueHeader() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(8080);
    request.setRequestURI("/mvc-showcase");
    request.addHeader("X-Forwarded-Host", "a.example.org");
    request.addHeader("X-Forwarded-Port", "443");
    request.addHeader("X-Forwarded-Proto", "https,https");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("https://a.example.org/mvc-showcase");
  }

  @Test
  void fromHttpRequestWithEmptyScheme() {
    HttpRequest request = new AbstractHttpRequest() {
      @Override
      public HttpMethod getMethod() {
        return HttpMethod.GET;
      }

      @Override
      public URI getURI() {
        return UriComponentsBuilder.fromUriString("/").build().toUri();
      }

      @Override
      public HttpHeaders getHeaders() {
        return HttpHeaders.forWritable();
      }
    };
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(request.getURI(), request.getHeaders()).build();

    assertThat(result.toString()).isEqualTo("/");
  }

  @Test
    // SPR-11856
  void fromHttpRequestForwardedHeader() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "proto=https; host=84.198.58.199");
    request.setScheme("http");
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
  }

  @Test
  void fromHttpRequestForwardedHeaderQuoted() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "proto=\"https\"; host=\"84.198.58.199\"");
    request.setScheme("http");
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
  }

  @Test
  void fromHttpRequestMultipleForwardedHeader() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "host=84.198.58.199;proto=https");
    request.addHeader("Forwarded", "proto=ftp; host=1.2.3.4");
    request.setScheme("http");
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
  }

  @Test
  void fromHttpRequestMultipleForwardedHeaderComma() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "host=84.198.58.199 ;proto=https, proto=ftp; host=1.2.3.4");
    request.setScheme("http");
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
  }

  @Test
  void fromHttpRequestForwardedHeaderWithHostPortAndWithoutServerPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "proto=https; host=84.198.58.199:9090");
    request.setScheme("http");
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
    assertThat(result.getPort()).isEqualTo(9090);
    assertThat(result.toUriString()).isEqualTo("https://84.198.58.199:9090/rest/mobile/users/1");
  }

  @Test
  void fromHttpRequestForwardedHeaderWithHostPortAndServerPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "proto=https; host=84.198.58.199:9090");
    request.setScheme("http");
    request.setServerPort(8080);
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
    assertThat(result.getPort()).isEqualTo(9090);
    assertThat(result.toUriString()).isEqualTo("https://84.198.58.199:9090/rest/mobile/users/1");
  }

  @Test
  void fromHttpRequestForwardedHeaderWithoutHostPortAndWithServerPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "proto=https; host=84.198.58.199");
    request.setScheme("http");
    request.setServerPort(8080);
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("84.198.58.199");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.toUriString()).isEqualTo("https://84.198.58.199/rest/mobile/users/1");
  }

  @Test
    // SPR-16262
  void fromHttpRequestForwardedHeaderWithProtoAndServerPort() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "proto=https");
    request.setScheme("http");
    request.setServerPort(8080);
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("example.com");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.toUriString()).isEqualTo("https://example.com/rest/mobile/users/1");
  }

  @Test
    // gh-25737
  void fromHttpRequestForwardedHeaderComma() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Forwarded", "for=192.0.2.0,for=192.0.2.1;proto=https;host=192.0.2.3:9090");
    request.setScheme("http");
    request.setServerPort(8080);
    request.setServerName("example.com");
    request.setRequestURI("/rest/mobile/users/1");

    HttpRequest httpRequest = new MockServerHttpRequest(request);
    UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("192.0.2.3");
    assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
    assertThat(result.getPort()).isEqualTo(9090);
    assertThat(result.toUriString()).isEqualTo("https://192.0.2.3:9090/rest/mobile/users/1");
  }

}