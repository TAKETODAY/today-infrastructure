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

package infra.web.socket.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.context.Lifecycle;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.web.ServerHttpExchange;
import infra.web.mock.MockServerHttpExchange;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.HandshakeHandler;
import infra.web.socket.server.HandshakeInterceptor;
import infra.web.socket.server.support.WebSocketHttpRequestHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketRegistrationTests {

  @Test
  void appliesRegistryOrderAndCustomHandshakeConfiguration() throws Exception {
    var registry = new DefaultWebSocketHandlerRegistry();
    var handshake = mock(HandshakeHandler.class);
    var handler = mock(WebSocketHandler.class);
    var interceptor = mock(HandshakeInterceptor.class);
    registry.setOrder(7);
    registry.setHandshakeHandler(handshake);
    var registration = (DefaultWebSocketHandlerRegistration) registry.addHandler(handler, "/one", "/two");
    registration.addInterceptors().addInterceptors(interceptor);
    registration.setAllowedOrigins("https://old.example").setAllowedOrigins();
    registration.setAllowedOriginPatterns();

    assertThat(registry.getOrder()).isEqualTo(7);
    assertThat(registration.getHandshakeHandler()).isSameAs(handshake);
    var mapping = registry.getHandlerMapping();
    assertThat(mapping.getOrder()).isEqualTo(7);
    assertThat(mapping.getUrlMap()).containsOnlyKeys("/one", "/two");

    var request = new MockRequest("GET", "/one");
    var context = new MockServerHttpExchange(null, request, new MockResponse());
    given(interceptor.beforeHandshake(context, handler, Map.of())).willReturn(true);
    var requestHandler = (WebSocketHttpRequestHandler) mapping.getUrlMap().get("/one");
    requestHandler.handleRequest(context);
    verify(handshake).doHandshake(context, handler, Map.of());
    verify(interceptor).afterHandshake(context, handler, null);
  }

  @Test
  void lifecycleIgnoresOrdinaryHandlersAndOnlyRunsOncePerTransition() {
    var mapping = new DefaultWebSocketHandlerRegistry().getHandlerMapping();
    var lifecycle = mock(Lifecycle.class);
    mapping.setUrlMap(Map.of("/managed", lifecycle, "/ordinary", new Object()));

    mapping.stop();
    mapping.start();
    mapping.start();
    assertThat(mapping.isRunning()).isTrue();
    verify(lifecycle).start();
    mapping.stop();
    mapping.stop();
    assertThat(mapping.isRunning()).isFalse();
    verify(lifecycle).stop();
  }

  @Test
  void upgradeMatchingDoesNotSuppressOrdinaryHandlers() throws Exception {
    var registry = new DefaultWebSocketHandlerRegistry();
    registry.addHandler(mock(WebSocketHandler.class), "/socket");
    var mapping = registry.getHandlerMapping();
    var ordinary = new Object();
    mapping.setUrlMap(Map.of("/ordinary", ordinary));
    mapping.setWebSocketUpgradeMatch(true);

    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();
      mapping.setApplicationContext(context);
      assertThat(mapping.getHandler(request("/ordinary", "other"))).isNotNull();
      assertThat(mapping.getHandler(request("/socket", "other"))).isNull();
      assertThat(mapping.getHandler(request("/socket", "WebSocket"))).isNotNull();
    }
  }

  private static ServerHttpExchange request(String path, String upgrade) {
    var request = new MockRequest("GET", path);
    request.addHeader("Upgrade", upgrade);
    return new MockServerHttpExchange(null, request, new MockResponse());
  }

}
