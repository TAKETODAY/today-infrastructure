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

package infra.web.socket.handler;

import java.util.Map;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.RequestContext;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;
import infra.web.socket.server.HandshakeCapable;

/**
 * A {@link WebSocketHandler} that adds logging to WebSocket lifecycle events.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/12 16:49
 * @since 4.0
 */
public class LoggingWebSocketHandler extends WebSocketHandler implements HandshakeCapable {

  private static final Logger logger = LoggerFactory.getLogger(LoggingWebSocketHandler.class);

  public LoggingWebSocketHandler(WebSocketHandler delegate) {
    super(delegate);
  }

  @Override
  public boolean beforeHandshake(RequestContext request, Map<String, Object> attributes) throws Throwable {
    if (logger.isDebugEnabled()) {
      logger.debug("Before Handshake {}", request);
    }
    if (delegate instanceof HandshakeCapable hc) {
      return hc.beforeHandshake(request, attributes);
    }
    return true;
  }

  @Override
  public void afterHandshake(RequestContext request, @Nullable WebSocketSession session, @Nullable Throwable failure) throws Throwable {
    if (failure != null) {
      logger.error("Handshake failed {}", request, failure);
    }
    else if (logger.isDebugEnabled()) {
      logger.debug("After Handshake {}", request);
    }
    if (delegate instanceof HandshakeCapable hc) {
      hc.afterHandshake(request, session, failure);
    }
  }

  @Override
  public void onOpen(WebSocketSession session) throws Throwable {
    if (logger.isDebugEnabled()) {
      logger.debug("New {}", session);
    }
    super.onOpen(session);
  }

  @Override
  public void handleMessage(WebSocketSession session, WebSocketMessage message) throws Throwable {
    if (logger.isTraceEnabled()) {
      logger.trace("Handling {} in {}", message, session);
    }
    super.handleMessage(session, message);
  }

  @Override
  public void onError(WebSocketSession session, Throwable throwable) throws Throwable {
    if (logger.isDebugEnabled()) {
      logger.debug("Transport error in {}", session, throwable);
    }
    super.onError(session, throwable);
  }

  @Override
  public void onClose(WebSocketSession session, CloseStatus status) throws Throwable {
    if (logger.isDebugEnabled()) {
      logger.debug("{} closed with {}", session, status);
    }
    super.onClose(session, status);
  }

}
