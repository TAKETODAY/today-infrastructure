/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.socket.handler;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * A {@link WebSocketHandler} that adds logging to WebSocket lifecycle events.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/12 16:49
 * @since 4.0
 */
public class LoggingWebSocketHandlerDecorator extends WebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(LoggingWebSocketHandlerDecorator.class);

  public LoggingWebSocketHandlerDecorator(WebSocketHandler delegate) {
    super(delegate);
  }

  @Override
  public void onOpen(WebSocketSession session) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("New {}", session);
    }
    super.onOpen(session);
  }

  @Override
  public void handleMessage(WebSocketSession session, Message<?> message) throws Exception {
    if (logger.isTraceEnabled()) {
      logger.trace("Handling {} in {}", message, session);
    }
    super.handleMessage(session, message);
  }

  @Override
  public void onError(WebSocketSession session, Throwable throwable) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("Transport error in {}", session, throwable);
    }
    super.onError(session, throwable);
  }

  @Override
  public void onClose(WebSocketSession session, CloseStatus status) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("{} closed with {}", session, status);
    }
    super.onClose(session, status);
  }

}
