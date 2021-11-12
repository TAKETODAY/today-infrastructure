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
package cn.taketoday.web.socket;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * A {@link WebSocketHandlerDecorator} that adds logging to WebSocket lifecycle events.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/12 16:49
 * @since 4.0
 */
public class LoggingWebSocketHandlerDecorator extends WebSocketHandlerDecorator {
  private static final Logger logger = LoggerFactory.getLogger(LoggingWebSocketHandlerDecorator.class);

  public LoggingWebSocketHandlerDecorator(WebSocketHandler delegate) {
    super(delegate);
  }

  @Override
  public void onOpen(WebSocketSession session) {
    if (logger.isDebugEnabled()) {
      logger.debug("New {}", session);
    }
    super.onOpen(session);
  }

  @Override
  public void handleMessage(WebSocketSession session, Message<?> message) {
    if (logger.isTraceEnabled()) {
      logger.trace("Handling {} in {}", message, session);
    }
    super.handleMessage(session, message);
  }

  @Override
  public void onError(WebSocketSession session, Throwable throwable) {
    if (logger.isDebugEnabled()) {
      logger.debug("Transport error in {}", session, throwable);
    }
    super.onError(session, throwable);
  }

  @Override
  public void onClose(WebSocketSession session, CloseStatus status) {
    if (logger.isDebugEnabled()) {
      logger.debug("{} closed with {}", session, status);
    }
    super.onClose(session, status);
  }

}
