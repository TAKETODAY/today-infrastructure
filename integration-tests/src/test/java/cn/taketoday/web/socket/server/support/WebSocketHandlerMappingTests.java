/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.socket.server.support;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.socket.WebSocketHandler;

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

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
    ServletRequestContext context = new ServletRequestContext(null, request, new MockHttpServletResponse());
    HandlerExecutionChain chain = (HandlerExecutionChain) mapping.getHandler(context);
    assertThat(chain).isNotNull();
    assertThat(chain.getHandler()).isSameAs(handler);

    mapping.setWebSocketUpgradeMatch(true);

    chain = (HandlerExecutionChain) mapping.getHandler(context);
    assertThat(chain).isNull();

    request.addHeader("Upgrade", "websocket");
    context = new ServletRequestContext(null, request, new MockHttpServletResponse());
    chain = (HandlerExecutionChain) mapping.getHandler(context);
    assertThat(chain).isNotNull();
    assertThat(chain.getHandler()).isSameAs(handler);

    request.setMethod("POST");
    context = new ServletRequestContext(null, request, new MockHttpServletResponse());
    chain = (HandlerExecutionChain) mapping.getHandler(context);
    assertThat(chain).isNull();
  }

}
