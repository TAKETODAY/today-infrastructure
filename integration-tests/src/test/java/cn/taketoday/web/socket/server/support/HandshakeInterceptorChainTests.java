/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.web.socket.AbstractHttpRequestTests;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.server.HandshakeInterceptor;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test fixture for {@link HandshakeInterceptorChain}.
 *
 * @author Rossen Stoyanchev
 */
public class HandshakeInterceptorChainTests extends AbstractHttpRequestTests {

  private HandshakeInterceptor i1;

  private HandshakeInterceptor i2;

  private HandshakeInterceptor i3;

  private List<HandshakeInterceptor> interceptors;

  private WebSocketHandler wsHandler;

  private Map<String, Object> attributes;

  @Override
  @BeforeEach
  public void setup() {
    super.setup();

    i1 = mock(HandshakeInterceptor.class);
    i2 = mock(HandshakeInterceptor.class);
    i3 = mock(HandshakeInterceptor.class);
    interceptors = Arrays.asList(i1, i2, i3);
    wsHandler = mock(WebSocketHandler.class);
    attributes = new HashMap<>();
  }

  @Test
  public void success() throws Exception {
    given(i1.beforeHandshake(request, wsHandler, attributes)).willReturn(true);
    given(i2.beforeHandshake(request, wsHandler, attributes)).willReturn(true);
    given(i3.beforeHandshake(request, wsHandler, attributes)).willReturn(true);

    HandshakeInterceptorChain chain = new HandshakeInterceptorChain(interceptors, wsHandler);
    chain.applyBeforeHandshake(request, attributes);

    verify(i1).beforeHandshake(request, wsHandler, attributes);
    verify(i2).beforeHandshake(request, wsHandler, attributes);
    verify(i3).beforeHandshake(request, wsHandler, attributes);
    verifyNoMoreInteractions(i1, i2, i3);
  }

  @Test
  public void applyBeforeHandshakeWithFalseReturnValue() throws Exception {
    given(i1.beforeHandshake(request, wsHandler, attributes)).willReturn(true);
    given(i2.beforeHandshake(request, wsHandler, attributes)).willReturn(false);

    HandshakeInterceptorChain chain = new HandshakeInterceptorChain(interceptors, wsHandler);
    chain.applyBeforeHandshake(request, attributes);

    verify(i1).beforeHandshake(request, wsHandler, attributes);
    verify(i1).afterHandshake(request, wsHandler, null);
    verify(i2).beforeHandshake(request, wsHandler, attributes);
    verifyNoMoreInteractions(i1, i2, i3);
  }

  @Test
  public void applyAfterHandshakeOnly() {
    HandshakeInterceptorChain chain = new HandshakeInterceptorChain(interceptors, wsHandler);
    chain.applyAfterHandshake(request, null);

    verifyNoMoreInteractions(i1, i2, i3);
  }

}
