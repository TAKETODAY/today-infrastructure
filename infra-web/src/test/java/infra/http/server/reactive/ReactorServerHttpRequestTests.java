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

package infra.http.server.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.http.HttpCookie;
import infra.http.HttpMethod;
import infra.util.MultiValueMap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import reactor.netty.http.server.HttpServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 00:40
 */
class ReactorServerHttpRequestTests {

  private HttpServerRequest mockRequest;

  private NettyDataBufferFactory bufferFactory;

  @BeforeEach
  void setUp() {
    mockRequest = mock(HttpServerRequest.class);
    bufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
  }

  @Test
  void constructor_shouldInitializeFields() throws URISyntaxException {
    HttpMethod method = HttpMethod.GET;
    URI uri = URI.create("http://example.com/path");
    DefaultHttpHeaders nettyHeaders = new DefaultHttpHeaders();
    nettyHeaders.add("Content-Type", "application/json");

    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/path");
    when(mockRequest.requestHeaders()).thenReturn(nettyHeaders);
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    infra.http.server.reactive.ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    assertThat(request.getMethod()).isEqualTo(method);
    assertThat(request.getURI()).isEqualTo(uri);
  }

  @Test
  void initCookies_shouldParseNettyCookies() throws URISyntaxException {
    Cookie cookie1 = new DefaultCookie("name1", "value1");
    Cookie cookie2 = new DefaultCookie("name2", "value2");
    Map<CharSequence, List<Cookie>> allCookies = Map.of(
            "name1", List.of(cookie1),
            "name2", List.of(cookie2)
    );

    when(mockRequest.allCookies()).thenReturn(allCookies);
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    assertThat(request.getCookies()).hasSize(2);
    assertThat(request.getCookies().getFirst("name1").getValue()).isEqualTo("value1");
    assertThat(request.getCookies().getFirst("name2").getValue()).isEqualTo("value2");
  }

  @Test
  void getLocalAddress_shouldReturnHostAddress() throws URISyntaxException {
    InetSocketAddress localAddress = new InetSocketAddress("localhost", 8080);
    when(mockRequest.hostAddress()).thenReturn(localAddress);
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    assertThat(request.getLocalAddress()).isEqualTo(localAddress);
  }

  @Test
  void getRemoteAddress_shouldReturnRemoteAddress() throws URISyntaxException {
    InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.1", 12345);
    when(mockRequest.remoteAddress()).thenReturn(remoteAddress);
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    assertThat(request.getRemoteAddress()).isEqualTo(remoteAddress);
  }

  @Test
  void getNativeRequest_shouldReturnOriginalRequest() throws URISyntaxException {
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    Object nativeRequest = request.getNativeRequest();
    assertThat(nativeRequest).isSameAs(mockRequest);
  }

  @Test
  void getMethodValue_shouldReturnNettyMethod() throws URISyntaxException {
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.POST);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    assertThat(request.getMethodAsString()).isEqualTo("POST");
  }

  @Test
  void initCookies_withEmptyCookies_shouldReturnEmptyMap() throws URISyntaxException {
    when(mockRequest.allCookies()).thenReturn(Map.of());
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
    assertThat(cookies).isEmpty();
  }

  @Test
  void initCookies_withMultipleCookiesSameName_shouldAddAll() throws URISyntaxException {
    Cookie cookie1 = new DefaultCookie("name", "value1");
    Cookie cookie2 = new DefaultCookie("name", "value2");
    Map<CharSequence, List<Cookie>> allCookies = Map.of("name", List.of(cookie1, cookie2));

    when(mockRequest.allCookies()).thenReturn(allCookies);
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    List<HttpCookie> cookies = request.getCookies().get("name");
    assertThat(cookies).hasSize(2);
    assertThat(cookies.get(0).getValue()).isEqualTo("value1");
    assertThat(cookies.get(1).getValue()).isEqualTo("value2");
  }

  @Test
  void getLocalAddress_whenNull_shouldReturnNull() throws URISyntaxException {
    when(mockRequest.hostAddress()).thenReturn(null);
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    assertThat(request.getLocalAddress()).isNull();
  }

  @Test
  void getRemoteAddress_whenNull_shouldReturnNull() throws URISyntaxException {
    when(mockRequest.remoteAddress()).thenReturn(null);
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    assertThat(request.getRemoteAddress()).isNull();
  }

  @Test
  void initId_withoutConnection_shouldReturnNull() throws URISyntaxException {
    when(mockRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
    when(mockRequest.uri()).thenReturn("/");
    when(mockRequest.requestHeaders()).thenReturn(new DefaultHttpHeaders());
    when(mockRequest.scheme()).thenReturn("http");
    when(mockRequest.hostName()).thenReturn("example.com");
    when(mockRequest.hostPort()).thenReturn(80);

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

    String id = request.initId();
    assertThat(id).isNull();
  }

}
