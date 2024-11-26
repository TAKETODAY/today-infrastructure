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

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.socket.CloseStatus;
import infra.web.socket.Message;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;

/**
 * An exception handling {@link WebSocketHandler}.
 * Traps all {@link Throwable} instances that escape from the decorated
 * handler and closes the session with {@link CloseStatus#SERVER_ERROR}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/7 21:47
 */
public class ExceptionWebSocketHandlerDecorator extends WebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(ExceptionWebSocketHandlerDecorator.class);

  public ExceptionWebSocketHandlerDecorator(WebSocketHandler delegate) {
    super(delegate);
  }

  @Override
  public void onOpen(WebSocketSession session) {
    try {
      super.onOpen(session);
    }
    catch (Exception ex) {
      tryCloseWithError(session, ex, logger);
    }
  }

  @Override
  public void handleMessage(WebSocketSession session, Message<?> message) {
    try {
      super.handleMessage(session, message);
    }
    catch (Exception ex) {
      tryCloseWithError(session, ex, logger);
    }
  }

  @Override
  public void onError(WebSocketSession session, Throwable exception) {
    try {
      super.onError(session, exception);
    }
    catch (Exception ex) {
      tryCloseWithError(session, ex, logger);
    }
  }

  @Override
  public void onClose(WebSocketSession session, CloseStatus closeStatus) {
    try {
      super.onClose(session, closeStatus);
    }
    catch (Exception ex) {
      logger.warn("Unhandled exception after connection closed for {}", this, ex);
    }
  }

  public static void tryCloseWithError(WebSocketSession session, Throwable exception, Logger logger) {
    if (logger.isErrorEnabled()) {
      logger.error("Closing session due to exception for {}", session, exception);
    }
    if (session.isOpen()) {
      try {
        session.close(CloseStatus.SERVER_ERROR);
      }
      catch (Throwable ex) {
        // ignore
      }
    }
  }

}
