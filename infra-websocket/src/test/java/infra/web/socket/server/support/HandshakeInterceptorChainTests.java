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

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import infra.web.HttpContext;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.HandshakeCapable;
import infra.web.socket.server.HandshakeInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class HandshakeInterceptorChainTests {

  private final HttpContext request = mock(HttpContext.class);

  private final Map<String, Object> attributes = Map.of();

  @Test
  void acceptsHandshakeWithoutInterceptors() throws Exception {
    var chain = new HandshakeInterceptorChain(null, mock(WebSocketHandler.class));
    assertThat(chain.applyBeforeHandshake(request, attributes)).isTrue();
    chain.applyAfterHandshake(request, null, null);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void unwindsCompletedInterceptorsEvenWhenCleanupThrows(boolean verbose) throws Exception {
    var handler = mock(HandshakeAwareHandler.class);
    var first = mock(HandshakeInterceptor.class);
    var second = mock(HandshakeInterceptor.class);
    given(first.beforeHandshake(request, handler, attributes)).willReturn(true);
    given(second.beforeHandshake(request, handler, attributes)).willReturn(true);
    doThrow(new IllegalStateException("cleanup failed")).when(second).afterHandshake(request, handler, null);
    Logger logger = (Logger) LoggerFactory.getLogger(HandshakeInterceptorChain.class);
    Level previous = logger.getLevel();
    logger.setLevel(verbose ? Level.DEBUG : Level.OFF);
    try {
      var chain = new HandshakeInterceptorChain(List.of(first, second), handler);
      assertThat(chain.applyBeforeHandshake(request, attributes)).isFalse();
      var order = inOrder(first, second, handler);
      order.verify(first).beforeHandshake(request, handler, attributes);
      order.verify(second).beforeHandshake(request, handler, attributes);
      order.verify(handler).beforeHandshake(request, attributes);
      order.verify(handler).afterHandshake(request, null, null);
      order.verify(second).afterHandshake(request, handler, null);
      order.verify(first).afterHandshake(request, handler, null);

      var veto = mock(HandshakeInterceptor.class);
      var skipped = mock(HandshakeInterceptor.class);
      var rejected = new HandshakeInterceptorChain(List.of(veto, skipped), handler);
      assertThat(rejected.applyBeforeHandshake(request, attributes)).isFalse();
      verifyNoInteractions(skipped);
    }
    finally {
      logger.setLevel(previous);
    }
  }

  private abstract static class HandshakeAwareHandler extends WebSocketHandler implements HandshakeCapable {
  }

}
