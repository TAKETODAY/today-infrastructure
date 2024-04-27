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

package cn.taketoday.web.socket.server.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.server.HandshakeFailureException;
import cn.taketoday.web.socket.server.HandshakeHandler;
import cn.taketoday.web.socket.server.HandshakeInterceptor;
import cn.taketoday.web.mock.ServletException;

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

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @Test
  public void success() throws Throwable {
    TestInterceptor interceptor = new TestInterceptor(true);
    this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));
    ServletRequestContext request = new ServletRequestContext(
            null, new MockHttpServletRequest(), this.response);
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

    ServletRequestContext request = new ServletRequestContext(
            null, new MockHttpServletRequest(), this.response);
    assertThatThrownBy(() -> this.requestHandler.handleRequest(request))
            .isInstanceOf(HandshakeFailureException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasMessageEndingWith("bad state");

    request.requestCompleted();

    assertThat(this.response.getHeader("headerName")).isEqualTo("headerValue");
    assertThat(this.response.getHeader("exceptionHeaderName")).isEqualTo("exceptionHeaderValue");
  }

  @Test // gh-23179
  public void handshakeNotAllowed() throws Throwable {
    TestInterceptor interceptor = new TestInterceptor(false);
    this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));

    ServletRequestContext request = new ServletRequestContext(
            null, new MockHttpServletRequest(), this.response);
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
            WebSocketHandler wsHandler, Exception exception) {

      request.responseHeaders().add("exceptionHeaderName", "exceptionHeaderValue");
    }
  }

}
