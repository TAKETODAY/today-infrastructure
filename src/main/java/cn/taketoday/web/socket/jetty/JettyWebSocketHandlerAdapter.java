/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.socket.jetty;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletContextAware;
import cn.taketoday.web.exception.WebNestedRuntimeException;
import cn.taketoday.web.socket.AbstractWebSocketHandlerAdapter;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.utils.ServletUtils;

/**
 * @author TODAY 2021/5/6 21:21
 * @since 3.0.1
 */
public class JettyWebSocketHandlerAdapter
        extends AbstractWebSocketHandlerAdapter implements ServletContextAware, DisposableBean {

  private WebSocketServerFactory webSocketServerFactory;

  private WebSocketPolicy policy;
  private ByteBufferPool bufferPool;

  @Override
  protected void doHandshake(
          RequestContext context, final WebSocketSession session, WebSocketHandler handler) throws Throwable {
    final HttpServletRequest servletRequest = ServletUtils.getServletRequest(context);
    final HttpServletResponse servletResponse = ServletUtils.getServletResponse(context);

    final class WebSocketCreator0 implements WebSocketCreator {
      @Override
      public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        if (handler.supportPartialMessage()) {
          return new JettyPartialWebSocketConnectionListener((JettyWebSocketSession) session, handler);
        }
        return new JettyWebSocketConnectionListener((JettyWebSocketSession) session, handler);
      }
    }

    webSocketServerFactory.acceptWebSocket(new WebSocketCreator0(), servletRequest, servletResponse);
  }

  @Override
  protected WebSocketSession createSession(RequestContext context, WebSocketHandler handler) {
    return new JettyWebSocketSession();
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    if (policy == null) {
      policy = WebSocketPolicy.newServerPolicy();
    }
    if (bufferPool == null) {
      bufferPool = new MappedByteBufferPool();
    }
    this.webSocketServerFactory = new WebSocketServerFactory(servletContext, policy, bufferPool);
    try {
      webSocketServerFactory.start();
    }
    catch (Exception e) {
      throw new WebNestedRuntimeException("WebSocketServerFactory cannot start successfully");
    }
  }

  public void setPolicy(WebSocketPolicy policy) {
    this.policy = policy;
  }

  public void setWebSocketServerFactory(WebSocketServerFactory webSocketServerFactory) {
    this.webSocketServerFactory = webSocketServerFactory;
  }

  public void setBufferPool(ByteBufferPool bufferPool) {
    this.bufferPool = bufferPool;
  }

  public ByteBufferPool getBufferPool() {
    return bufferPool;
  }

  public WebSocketPolicy getPolicy() {
    return policy;
  }

  @Override
  public void destroy() throws Exception {
    if (webSocketServerFactory != null) {
      webSocketServerFactory.stop();
    }
  }
}
