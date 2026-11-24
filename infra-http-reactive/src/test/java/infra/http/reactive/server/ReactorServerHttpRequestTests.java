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

package infra.http.reactive.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.http.HttpCookie;
import infra.http.HttpMethod;
import infra.util.MultiValueMap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelId;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.ssl.SslHandler;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.ChannelOperationsId;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServerRequest;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

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

    ReactorServerHttpRequest request = new ReactorServerHttpRequest(mockRequest, bufferFactory);

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

  @Test
  void bodyRetainsNativeBufferForDownstream() throws URISyntaxException {
    var byteBuf = Unpooled.copiedBuffer("request body", StandardCharsets.UTF_8);
    given(mockRequest.receive()).willReturn(ByteBufFlux.fromInbound(Mono.just(byteBuf)));
    ReactorServerHttpRequest request = createRequest(mockRequest, "/");

    try {
      StepVerifier.create(request.getBody())
              .assertNext(buffer -> {
                try {
                  assertThat(buffer.toString(StandardCharsets.UTF_8)).isEqualTo("request body");
                  assertThat(byteBuf.refCnt()).isEqualTo(2);
                }
                finally {
                  buffer.release();
                }
              })
              .expectComplete().verify(Duration.ofSeconds(5));
      assertThat(byteBuf.refCnt()).isEqualTo(1);
    }
    finally {
      byteBuf.release();
    }
  }

  @Test
  void usesChannelIdWhenOperationsIdIsUnavailable() throws URISyntaxException {
    HttpServerRequest nativeRequest = mock(HttpServerRequest.class,
            withSettings().extraInterfaces(Connection.class, ChannelOperationsId.class));
    Channel channel = mock(Channel.class);
    DefaultChannelId channelId = DefaultChannelId.newInstance();
    given(((Connection) nativeRequest).channel()).willReturn(channel);
    given(channel.id()).willReturn(channelId);
    ReactorServerHttpRequest request = createRequest(nativeRequest, "/");

    String requestId = request.getId();
    assertThat(requestId).startsWith(channelId.asShortText() + "-");
    assertThat(request.getId()).isEqualTo(requestId);
    String logPrefix = request.getLogPrefix();
    assertThat(logPrefix).startsWith("[" + channelId.asShortText() + "-").endsWith("] ");
    assertThat(request.getLogPrefix()).isEqualTo(logPrefix);
  }

  @Test
  void prefersOperationsIdForLogPrefix() throws URISyntaxException {
    HttpServerRequest nativeRequest = mock(HttpServerRequest.class,
            withSettings().extraInterfaces(ChannelOperationsId.class));
    ChannelOperationsId operationsId = (ChannelOperationsId) nativeRequest;
    given(operationsId.asShortText()).willReturn("operation");
    given(operationsId.asLongText()).willReturn("channel-operation");
    ReactorServerHttpRequest request = createRequest(nativeRequest, "/");

    assertThat(request.getLogPrefix()).isIn("[operation] ", "[channel-operation] ");
  }

  @Test
  void fallsBackToRequestIdWithoutConnection() throws URISyntaxException {
    ReactorServerHttpRequest request = createRequest(mockRequest, "/");

    assertThat(request.getId()).isNotBlank();
    assertThat(request.getLogPrefix()).isEqualTo("[" + request.getId() + "] ");
  }

  @Test
  void mutatedRequestExposesOriginalNativeRequest() throws URISyntaxException {
    HttpServerRequest nativeRequest = mock(HttpServerRequest.class, withSettings().extraInterfaces(Connection.class));
    Channel channel = mock(Channel.class);
    given(((Connection) nativeRequest).channel()).willReturn(channel);
    given(channel.pipeline()).willReturn(mock(ChannelPipeline.class));
    given(nativeRequest.receive()).willReturn(ByteBufFlux.fromInbound(Mono.empty()));

    ServerHttpRequest mutated = createRequest(nativeRequest, "/").mutate().build();

    assertThat(DelegatingServerHttpRequest.<HttpServerRequest>getNativeRequest(mutated)).isSameAs(nativeRequest);
  }

  @Test
  void getsSslSessionFromParentChannel() throws Exception {
    HttpServerRequest nativeRequest = mock(HttpServerRequest.class, withSettings().extraInterfaces(Connection.class));
    Channel child = mock(Channel.class);
    Channel parent = mock(Channel.class);
    ChannelPipeline parentPipeline = mock(ChannelPipeline.class);
    SslHandler sslHandler = mock(SslHandler.class);
    SSLEngine engine = mock(SSLEngine.class);
    SSLSession session = mock(SSLSession.class);
    given(((Connection) nativeRequest).channel()).willReturn(child);
    given(child.pipeline()).willReturn(mock(ChannelPipeline.class));
    given(child.parent()).willReturn(parent);
    given(parent.pipeline()).willReturn(parentPipeline);
    given(parentPipeline.get(SslHandler.class)).willReturn(sslHandler);
    given(sslHandler.engine()).willReturn(engine);
    given(engine.getSession()).willReturn(session);
    given(session.getId()).willReturn(new byte[] { 1, (byte) 0xAB });
    given(session.getPeerCertificates()).willThrow(new SSLPeerUnverifiedException("No peer certificate"));
    ReactorServerHttpRequest request = createRequest(nativeRequest, "/");

    SslInfo sslInfo = request.getSslInfo();
    assertThat(sslInfo).isNotNull();
    assertThat(sslInfo.getSessionId()).isEqualTo("01ab");
    assertThat(sslInfo.getPeerCertificates()).isNull();
    assertThat(request.getSslInfo()).isSameAs(sslInfo);
  }

  @Test
  void hasNoSslInfoOnPlainChannel() throws URISyntaxException {
    HttpServerRequest nativeRequest = mock(HttpServerRequest.class, withSettings().extraInterfaces(Connection.class));
    Channel channel = mock(Channel.class);
    given(((Connection) nativeRequest).channel()).willReturn(channel);
    given(channel.pipeline()).willReturn(mock(ChannelPipeline.class));

    assertThat(createRequest(nativeRequest, "/").getSslInfo()).isNull();
  }

  @Test
  void queryParamsDistinguishFlagsFromEmptyValuesAndDecodeRepeatedNames() throws URISyntaxException {
    ReactorServerHttpRequest request = createRequest(mockRequest, "/?na%6De=a+b&flag&empty=&name=%2F");

    MultiValueMap<String, String> params = request.getQueryParams();
    assertThat(params.get("name")).containsExactly("a b", "/");
    assertThat(params.get("flag")).containsExactly((String) null);
    assertThat(params.get("empty")).containsExactly("");
    assertThat(request.getQueryParams()).isSameAs(params);
  }

  private ReactorServerHttpRequest createRequest(HttpServerRequest nativeRequest, String uri) throws URISyntaxException {
    given(nativeRequest.method()).willReturn(io.netty.handler.codec.http.HttpMethod.GET);
    given(nativeRequest.uri()).willReturn(uri);
    given(nativeRequest.requestHeaders()).willReturn(new DefaultHttpHeaders());
    given(nativeRequest.scheme()).willReturn("http");
    given(nativeRequest.hostName()).willReturn("example.com");
    given(nativeRequest.hostPort()).willReturn(80);
    return new ReactorServerHttpRequest(nativeRequest, bufferFactory);
  }

}
