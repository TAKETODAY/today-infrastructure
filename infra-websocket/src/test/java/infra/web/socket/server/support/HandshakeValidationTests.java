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

package infra.web.socket.server.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.web.HttpContext;
import infra.web.socket.SubProtocolCapable;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketHttpHeaders;
import infra.web.socket.WebSocketSession;
import infra.web.socket.handler.LoggingWebSocketHandler;
import infra.web.socket.server.HandshakeFailureException;
import infra.web.socket.server.RequestUpgradeStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class HandshakeValidationTests {

  private final RequestUpgradeStrategy strategy = mock(RequestUpgradeStrategy.class);

  private final AbstractHandshakeHandler handshake = new AbstractHandshakeHandler(strategy) { };

  private final WebSocketHandler handler = new WebSocketHandler() { };

  private final HttpContext context = mock(HttpContext.class);

  private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

  private final Map<String, Object> attributes = Map.of("test", "value");

  @ParameterizedTest
  @ValueSource(strings = { "TRACE", "OFF" })
  void rejectsUnsupportedMethodAndAdvertisesAllowedMethods(String level) {
    HttpHeaders response = HttpHeaders.forWritable();
    given(context.getMethod()).willReturn(HttpMethod.POST);
    given(context.getHeaders()).willReturn(headers);
    given(context.responseHeaders()).willReturn(response);

    withLogLevel(level, () -> assertThat(handshake.doHandshake(context, handler, attributes)).isNull());

    verify(context).setStatus(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.CONNECT);
    verifyNoInteractions(strategy);
  }

  @ParameterizedTest
  @ValueSource(strings = { "TRACE", "OFF" })
  void rejectsInvalidGetHeaders(String level) throws IOException {
    prepareGet();
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    given(context.getOutputStream()).willReturn(body);

    withLogLevel(level, () -> {
      headers.remove(HttpHeaders.UPGRADE);
      assertThat(handshake.doHandshake(context, handler, attributes)).isNull();
      assertThat(body.toString(StandardCharsets.UTF_8)).contains("only to \"WebSocket\"");

      headers.setUpgrade("websocket");
      headers.setConnection("keep-alive");
      body.reset();
      assertThat(handshake.doHandshake(context, handler, attributes)).isNull();
      assertThat(body.toString(StandardCharsets.UTF_8)).contains("must be \"upgrade\"");

      headers.setConnection("upgrade");
      headers.remove(HttpHeaders.SEC_WEBSOCKET_KEY);
      body.reset();
      assertThat(handshake.doHandshake(context, handler, attributes)).isNull();
      assertThat(body.size()).isZero();
    });

    verifyNoInteractions(strategy);
  }

  @ParameterizedTest
  @ValueSource(strings = { "TRACE", "OFF" })
  void rejectsUnsupportedVersion(String level) {
    prepareGet();
    headers.setSecWebSocketVersion("8");
    given(strategy.getSupportedVersions()).willReturn(new String[] { "13", "12" });

    withLogLevel(level, () -> assertThat(handshake.doHandshake(context, handler, attributes)).isNull());

    verify(context).setStatus(HttpStatus.UPGRADE_REQUIRED);
    verify(context).setHeader(HttpHeaders.SEC_WEBSOCKET_VERSION, "13,12");
  }

  @Test
  void rejectsDisallowedOriginBeforeUpgrade() {
    prepareGet();
    given(strategy.getSupportedVersions()).willReturn(new String[] { "13" });
    AbstractHandshakeHandler restrictive = new AbstractHandshakeHandler(strategy) {
      @Override
      protected boolean isValidOrigin(HttpContext request) {
        return false;
      }
    };

    assertThat(restrictive.doHandshake(context, handler, attributes)).isNull();
    verify(context).setStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void wrapsIoFailureWhileWritingRejection() throws IOException {
    prepareGet();
    headers.remove(HttpHeaders.UPGRADE);
    IOException failure = new IOException("output unavailable");
    given(context.getURI()).willReturn(URI.create("https://example.test/socket"));
    given(context.getOutputStream()).willThrow(failure);

    assertThatThrownBy(() -> handshake.doHandshake(context, handler, attributes))
            .isInstanceOf(HandshakeFailureException.class).hasCause(failure).hasMessageContaining("/socket");
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void negotiatesProtocolAndExtensionsForGetAndConnect(boolean connect) {
    prepareGet();
    if (connect) {
      given(context.getMethod()).willReturn(HttpMethod.CONNECT);
      headers.remove(HttpHeaders.UPGRADE);
      headers.remove(HttpHeaders.CONNECTION);
      headers.remove(HttpHeaders.SEC_WEBSOCKET_KEY);
    }
    WebSocketExtension extension = new WebSocketExtension("permessage-deflate");
    headers.setSecWebSocketExtensions(List.of(new WebSocketExtension("unsupported"), extension));
    headers.setSecWebSocketProtocol(List.of("unknown", "CHAT", "fallback"));
    handshake.setSupportedProtocols("Fallback");
    WebSocketHandler decorated = new LoggingWebSocketHandler(new ChatHandler());
    given(strategy.getSupportedVersions()).willReturn(new String[] { "8", " 13 " });
    given(strategy.getSupportedExtensions(context)).willReturn(List.of(extension));
    WebSocketSession session = mock(WebSocketSession.class);
    given(strategy.upgrade(context, "CHAT", List.of(extension), decorated, attributes)).willReturn(session);

    withLogLevel(connect ? "TRACE" : "OFF",
            () -> assertThat(handshake.doHandshake(context, decorated, attributes)).isSameAs(session));

    assertThat(handshake.getSupportedProtocols()).containsExactly("fallback");
    assertThat(handshake.getRequestUpgradeStrategy()).isSameAs(strategy);
  }

  private void prepareGet() {
    given(context.getMethod()).willReturn(HttpMethod.GET);
    given(context.getHeaders()).willReturn(headers);
    headers.setUpgrade("websocket");
    headers.setConnection("Upgrade");
    headers.setSecWebSocketKey("dGhlIHNhbXBsZSBub25jZQ==");
    headers.setSecWebSocketVersion("13");
  }

  private void withLogLevel(String level, Runnable action) {
    Logger logger = (Logger) LoggerFactory.getLogger(handshake.getClass());
    Level previous = logger.getLevel();
    logger.setLevel(Level.toLevel(level));
    try {
      action.run();
    }
    finally {
      logger.setLevel(previous);
    }
  }

  private static class ChatHandler extends WebSocketHandler implements SubProtocolCapable {

    @Override
    public List<String> getSubProtocols() {
      return List.of("chat");
    }
  }

}
