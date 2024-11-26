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
