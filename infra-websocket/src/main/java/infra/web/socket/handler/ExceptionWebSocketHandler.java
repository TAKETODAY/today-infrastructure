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

package infra.web.socket.handler;

import org.jspecify.annotations.Nullable;

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
public class ExceptionWebSocketHandler extends WebSocketHandler {

  private static final Logger logger = LoggerFactory.getLogger(ExceptionWebSocketHandler.class);

  public ExceptionWebSocketHandler(WebSocketHandler delegate) {
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
