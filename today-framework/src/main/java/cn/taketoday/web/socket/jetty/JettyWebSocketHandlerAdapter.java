/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;

import java.lang.reflect.UndeclaredThrowableException;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.socket.AbstractWebSocketHandlerAdapter;
import cn.taketoday.web.socket.HandshakeFailedException;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author TODAY 2021/5/6 21:21
 * @since 3.0.1
 */
public class JettyWebSocketHandlerAdapter extends AbstractWebSocketHandlerAdapter {

  @Override
  protected void doHandshake(
          RequestContext context, WebSocketSession session, WebSocketHandler handler) {
    HttpServletRequest servletRequest = ServletUtils.getServletRequest(context);
    HttpServletResponse servletResponse = ServletUtils.getServletResponse(context);

    JettyWebSocketHandler handlerAdapter = new JettyWebSocketHandler(handler, (JettyWebSocketSession) session);
    JettyWebSocketCreator webSocketCreator = (upgradeRequest, upgradeResponse) -> handlerAdapter;

    ServletContext servletContext = servletRequest.getServletContext();
    JettyWebSocketServerContainer container = JettyWebSocketServerContainer.getContainer(servletContext);
    try {
      container.upgrade(webSocketCreator, servletRequest, servletResponse);
    }
    catch (UndeclaredThrowableException ex) {
      throw new HandshakeFailedException("Failed to upgrade", ex.getUndeclaredThrowable());
    }
    catch (Exception ex) {
      throw new HandshakeFailedException("Failed to upgrade", ex);
    }
  }

  @Override
  protected WebSocketSession createSession(RequestContext context, WebSocketHandler handler) {
    return new JettyWebSocketSession();
  }

}
