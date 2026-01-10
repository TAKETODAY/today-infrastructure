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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.web.socket.AbstractHttpRequestTests;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.HandshakeInterceptor;

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
  public void success() throws Throwable {
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
  public void applyBeforeHandshakeWithFalseReturnValue() throws Throwable {
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
  public void applyAfterHandshakeOnly() throws Throwable {
    HandshakeInterceptorChain chain = new HandshakeInterceptorChain(interceptors, wsHandler);
    chain.applyAfterHandshake(request, null, null);

    verifyNoMoreInteractions(i1, i2, i3);
  }

}
