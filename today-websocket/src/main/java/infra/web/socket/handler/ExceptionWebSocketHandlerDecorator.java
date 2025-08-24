/*
 * Copyright 2017 - 2025 the original author or authors.
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

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
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
    catch (Throwable ex) {
      tryCloseWithError(session, ex, logger);
    }
  }

  @Nullable
  @Override
  public Future<Void> handleMessage(WebSocketSession session, WebSocketMessage message) {
    try {
      Future<Void> future = super.handleMessage(session, message);
      if (future != null) {
        future.onFailed(ex -> tryCloseWithError(session, ex, logger));
      }
      return future;
    }
    catch (Throwable ex) {
      tryCloseWithError(session, ex, logger);
    }
    return null;
  }

  @Override
  public void onError(WebSocketSession session, Throwable exception) {
    try {
      super.onError(session, exception);
    }
    catch (Throwable ex) {
      tryCloseWithError(session, ex, logger);
    }
  }

  @Nullable
  @Override
  public Future<Void> onClose(WebSocketSession session, CloseStatus closeStatus) {
    try {
      Future<Void> future = super.onClose(session, closeStatus);
      if (future != null) {
        future.onFailed(ex -> logger.warn("Unhandled exception after connection closed for {}", this, ex));
      }
      return future;
    }
    catch (Throwable ex) {
      logger.warn("Unhandled exception after connection closed for {}", this, ex);
    }
    return null;
  }

  public static void tryCloseWithError(WebSocketSession session, Throwable exception, Logger logger) {
    if (logger.isErrorEnabled()) {
      logger.error("Closing session due to exception for {}", session, exception);
    }
    if (session.isOpen()) {
      session.close(CloseStatus.SERVER_ERROR);
    }
  }

}
