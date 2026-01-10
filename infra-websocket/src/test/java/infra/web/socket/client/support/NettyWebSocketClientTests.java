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

package infra.web.socket.client.support;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslManagerBundle;
import infra.core.ssl.SslOptions;
import infra.http.HttpHeaders;
import infra.util.DataSize;
import infra.util.concurrent.Future;
import infra.web.client.ResponseErrorHandler;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.ssl.SslContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/2 22:24
 */
class NettyWebSocketClientTests {

  final NettyWebSocketClient client = new NettyWebSocketClient();

  @Test
  void config() {
    assertThat(client).extracting("failOnMissingResponse").isEqualTo(false);
    assertThat(client).extracting("parseHttpAfterConnectRequest").isEqualTo(false);
    assertThat(client).extracting("maxContentLength").isEqualTo(DataSize.ofKilobytes(64).toBytesInt());
    assertThat(client).extracting("maxContentLength").isEqualTo(DataSize.ofKilobytes(64).bytes().intValue());
    assertThat(client).extracting("closeOnExpectationFailed").isEqualTo(false);
    assertThat(client).extracting("httpDecoderConfig").isNotNull();
    assertThat(client).extracting("channelFactory").isNull();
    assertThat(client).extracting("eventLoopGroup").isNull();

    client.setFailOnMissingResponse(true);
    client.setParseHttpAfterConnectRequest(true);
    client.setMaxContentLength(DataSize.ofBytes(100));
    client.setCloseOnExpectationFailed(true);
    client.setHttpDecoderConfig(new HttpDecoderConfig());
    client.setChannelFactory(null);
    client.setEventLoopGroup(null);
    client.setConnectTimeout(Duration.ofSeconds(1));

    assertThat(client).extracting("failOnMissingResponse").isEqualTo(true);
    assertThat(client).extracting("parseHttpAfterConnectRequest").isEqualTo(true);
    assertThat(client).extracting("maxContentLength").isEqualTo(100);
    assertThat(client).extracting("closeOnExpectationFailed").isEqualTo(true);
    assertThat(client).extracting("httpDecoderConfig").isNotNull();
    assertThat(client).extracting("channelFactory").isNull();
    assertThat(client).extracting("eventLoopGroup").isNull();
    assertThat(client).extracting("connectTimeout").isEqualTo(Duration.ofSeconds(1));

    assertThatThrownBy(() ->
            client.setHttpDecoderConfig(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("httpDecoderConfig is required");

    assertThatThrownBy(() ->
            client.setConnectTimeout(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("connectTimeout is required");
  }

  @Test
  void configureSslViaBundle() throws Exception {
    var sslBundle = mock(SslBundle.class);
    var sslOptions = mock(SslOptions.class);
    var managers = mock(SslManagerBundle.class);

    when(sslBundle.getOptions()).thenReturn(sslOptions);
    when(sslBundle.getManagers()).thenReturn(managers);
    when(sslOptions.getCiphers()).thenReturn(new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA" });
    when(sslOptions.getEnabledProtocols()).thenReturn(new String[] { "TLSv1.2" });

    client.setSslBundle(sslBundle);
    client.setSslContext(null);

    var uri = URI.create("wss://localhost:8080/ws");
    var handler = mock(WebSocketHandler.class);

    client.doHandshakeInternal(handler, HttpHeaders.forWritable(), uri, List.of(), List.of());
  }

  @Test
  void configureSslViaContext() throws Exception {
    var sslContext = mock(SslContext.class);
    client.setSslContext(sslContext);
    client.setSslBundle(null);

    var uri = URI.create("wss://localhost:8080/ws");
    var handler = mock(WebSocketHandler.class);

    client.doHandshakeInternal(handler, HttpHeaders.forWritable(), uri, List.of(), List.of());
  }

  @Test
  void handshakeWithSubProtocolsAndExtensions() throws Exception {
    var uri = URI.create("ws://localhost:8080/ws");
    var handler = mock(WebSocketHandler.class);
    var subProtocols = List.of("protocol1", "protocol2");
    var extensions = List.of(mock(WebSocketExtension.class));

    client.doHandshakeInternal(handler, HttpHeaders.forWritable(), uri, subProtocols, extensions);
  }

  @Test
  void handshakeWithCustomHeaders() throws Exception {
    var uri = URI.create("ws://localhost:8080/ws");
    var handler = mock(WebSocketHandler.class);
    var headers = HttpHeaders.forWritable();
    headers.add("Authorization", "Bearer token");

    client.doHandshakeInternal(handler, headers, uri, List.of(), List.of());
  }

  @Test
  void handshakeWithEmptyUri() {
    var uri = URI.create("");
    var handler = mock(WebSocketHandler.class);

    assertThatThrownBy(() ->
            client.doHandshakeInternal(handler, HttpHeaders.forWritable(), uri, List.of(), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void configureNegativeContentLength() {
    assertThatThrownBy(() -> client.setMaxContentLength(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void configureIOThreads() {
    client.setIOThreadCount(4);
    client.setIOThreadPoolName("test-pool");

    assertThat(client).extracting("ioThreadCount").isEqualTo(4);
    assertThat(client).extracting("ioThreadPoolName").isEqualTo("test-pool");
  }

  @Test
  void handshakeWithNullSubProtocols() throws Exception {
    var uri = URI.create("ws://localhost:8080/ws");
    var handler = mock(WebSocketHandler.class);

    client.doHandshakeInternal(handler, HttpHeaders.forWritable(), uri, null, List.of());
  }

  @Test
  void handshakeWithNullExtensions() throws Exception {
    var uri = URI.create("ws://localhost:8080/ws");
    var handler = mock(WebSocketHandler.class);

    client.doHandshakeInternal(handler, HttpHeaders.forWritable(), uri, List.of(), null);
  }

  @Test
  void configureErrorHandler() {
    var errorHandler = mock(ResponseErrorHandler.class);
    client.setErrorHandler(errorHandler);

    assertThat(client).extracting("errorHandler").isSameAs(errorHandler);

    assertThatThrownBy(() -> client.setErrorHandler(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ResponseErrorHandler is required");
  }

  @Test
  void configureHandshakerFactory() {
    var factory = mock(ClientHandshakerFactory.class);
    client.setHandshakerFactory(factory);

    assertThat(client).extracting("handshakerFactory").isSameAs(factory);

    assertThatThrownBy(() -> client.setHandshakerFactory(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ClientHandshakerFactory is required");
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class MockTests {

    @InjectMocks
    NettyWebSocketClient client;

    @Mock
    private WebSocketHandler webSocketHandler;

    @Test
    void connectViaSecureWebsocketProtocol() {
      URI uri = URI.create("wss://localhost:8443/ws");
      WebSocketHandler handler = mock(WebSocketHandler.class);

      Future<WebSocketSession> future = client.connect(uri, null, handler);

      assertThat(future).isNotNull();
      assertThat(future.isDone()).isFalse();
    }

    @Test
    void throwsExceptionForInvalidProtocol() {
      URI uri = URI.create("http://localhost:8080/ws");
      WebSocketHandler handler = mock(WebSocketHandler.class);

      assertThatThrownBy(() -> client.connect(uri, null, handler))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Invalid scheme: http");
    }

    @Test
    void throwsExceptionForNullHandler() {
      URI uri = URI.create("ws://localhost:8080/ws");

      assertThatThrownBy(() -> client.connect(uri, null, null))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("WebSocketHandler is required");
    }

    @Test
    void throwsExceptionForNullUri() {
      WebSocketHandler handler = mock(WebSocketHandler.class);

      assertThatThrownBy(() -> client.connect((URI) null, null, handler))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("URI is required");
    }

    @Test
    void connectWithInvalidSchemeThrowsException() {
      URI uri = URI.create("http://localhost:8080/websocket");

      assertThrows(IllegalArgumentException.class,
              () -> client.connect(uri, null, webSocketHandler));
    }

    @Test
    void connectWithNullUriThrowsException() {
      assertThrows(IllegalArgumentException.class,
              () -> client.connect((URI) null, null, webSocketHandler));
    }

    @Test
    void connectWithNullHandlerThrowsException() {
      URI uri = URI.create("ws://localhost:8080/websocket");

      assertThrows(IllegalArgumentException.class,
              () -> client.connect(uri, null, null));
    }

    @Test
    void connectWithNullUriTemplateThrowsException() {
      assertThrows(IllegalArgumentException.class,
              () -> client.connect(webSocketHandler, null, 8080));
    }

  }

}