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

package infra.web.socket.server.support;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HttpRequestHandler;
import infra.web.handler.HandlerExecutionChain;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.socket.WebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link WebSocketHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketHandlerMappingTests {

  @Test
  void webSocketHandshakeMatch() throws Exception {
    HttpRequestHandler handler = new WebSocketHttpRequestHandler(mock(WebSocketHandler.class));

    WebSocketHandlerMapping mapping = new WebSocketHandlerMapping();
    mapping.setUrlMap(Collections.singletonMap("/path", handler));
    mapping.setApplicationContext(new StaticWebApplicationContext());

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    MockRequestContext context = new MockRequestContext(null, request, new MockHttpResponseImpl());
    HandlerExecutionChain chain = (HandlerExecutionChain) mapping.getHandler(context);
    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isSameAs(handler);

    mapping.setWebSocketUpgradeMatch(true);

    chain = (HandlerExecutionChain) mapping.getHandler(context);
    assertThat(chain).isNull();

    request.addHeader("Upgrade", "websocket");
    context = new MockRequestContext(null, request, new MockHttpResponseImpl());
    chain = (HandlerExecutionChain) mapping.getHandler(context);
    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isSameAs(handler);

    request.setMethod("POST");
    context = new MockRequestContext(null, request, new MockHttpResponseImpl());
    chain = (HandlerExecutionChain) mapping.getHandler(context);
    assertThat(chain).isNull();
  }

}
