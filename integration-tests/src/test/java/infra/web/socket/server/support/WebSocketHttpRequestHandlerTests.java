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
import java.util.Map;

import infra.lang.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.HandshakeHandler;
import infra.web.socket.server.HandshakeInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link WebSocketHttpRequestHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketHttpRequestHandlerTests {

  private final HandshakeHandler handshakeHandler = mock(HandshakeHandler.class);

  private final WebSocketHttpRequestHandler requestHandler = new WebSocketHttpRequestHandler(mock(WebSocketHandler.class), this.handshakeHandler);

  private final MockHttpResponseImpl response = new MockHttpResponseImpl();

  @Test
  public void success() throws Throwable {
    TestInterceptor interceptor = new TestInterceptor(true);
    this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));
    MockRequestContext request = new MockRequestContext(
            null, new HttpMockRequestImpl(), this.response);
    this.requestHandler.handleRequest(request);
    request.requestCompleted();

    verify(this.handshakeHandler).doHandshake(any(), any(), any());
    assertThat(this.response.getHeader("headerName")).isEqualTo("headerValue");
  }

  @Test
  public void failure() {
    TestInterceptor interceptor = new TestInterceptor(true);
    this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));

    given(this.handshakeHandler.doHandshake(any(), any(), any()))
            .willThrow(new IllegalStateException("bad state"));

    MockRequestContext request = new MockRequestContext(
            null, new HttpMockRequestImpl(), this.response);
    assertThatThrownBy(() -> this.requestHandler.handleRequest(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageEndingWith("bad state");

    request.requestCompleted();

    assertThat(this.response.getHeader("headerName")).isEqualTo("headerValue");
    assertThat(this.response.getHeader("exceptionHeaderName")).isEqualTo("exceptionHeaderValue");
  }

  @Test
  public void handshakeNotAllowed() throws Throwable {
    TestInterceptor interceptor = new TestInterceptor(false);
    this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));

    MockRequestContext request = new MockRequestContext(
            null, new HttpMockRequestImpl(), this.response);
    this.requestHandler.handleRequest(request);
    request.requestCompleted();

    verifyNoMoreInteractions(this.handshakeHandler);
    assertThat(this.response.getHeader("headerName")).isEqualTo("headerValue");
  }

  private static class TestInterceptor implements HandshakeInterceptor {

    private final boolean allowHandshake;

    private TestInterceptor(boolean allowHandshake) {
      this.allowHandshake = allowHandshake;
    }

    @Override
    public boolean beforeHandshake(RequestContext request,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {

      request.responseHeaders().add("headerName", "headerValue");
      return this.allowHandshake;
    }

    @Override
    public void afterHandshake(RequestContext request,
            WebSocketHandler wsHandler, @Nullable Throwable exception) {

      request.responseHeaders().add("exceptionHeaderName", "exceptionHeaderValue");
    }
  }

}
