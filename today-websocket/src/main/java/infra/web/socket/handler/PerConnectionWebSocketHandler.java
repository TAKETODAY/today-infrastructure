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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;

/**
 * A {@link WebSocketHandler} that initializes and destroys a {@link WebSocketHandler}
 * instance for each WebSocket connection and delegates all other methods to it.
 *
 * <p>Essentially create an instance of this class once, providing the type of
 * {@link WebSocketHandler} class to create for each connection, and then pass it to any
 * API method that expects a {@link WebSocketHandler}.
 *
 * <p>If initializing the target {@link WebSocketHandler} type requires a Infra
 * BeanFactory, then the {@link #setBeanFactory(BeanFactory)} property accordingly. Simply
 * declaring this class as an Infra bean will do that. Otherwise, {@link WebSocketHandler}
 * instances of the target type will be created using the default constructor.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/24 20:40
 */
public class PerConnectionWebSocketHandler extends WebSocketHandler implements BeanFactoryAware {

  private static final Logger logger = LoggerFactory.getLogger(PerConnectionWebSocketHandler.class);

  private final BeanCreatingHandlerProvider<WebSocketHandler> provider;

  private final Map<WebSocketSession, WebSocketHandler> handlers =
          new ConcurrentHashMap<>();

  public PerConnectionWebSocketHandler(Class<? extends WebSocketHandler> handlerType) {
    this.provider = new BeanCreatingHandlerProvider<>(handlerType);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.provider.setBeanFactory(beanFactory);
  }

  @Override
  public void onOpen(WebSocketSession session) throws Throwable {
    WebSocketHandler handler = this.provider.getHandler();
    this.handlers.put(session, handler);
    handler.onOpen(session);
  }

  @Nullable
  @Override
  public Future<Void> handleMessage(WebSocketSession session, WebSocketMessage message) {
    return getHandler(session).handleMessage(session, message);
  }

  @Override
  public void onError(WebSocketSession session, Throwable exception) throws Throwable {
    getHandler(session).onError(session, exception);
  }

  @Override
  @Nullable
  public Future<Void> onClose(WebSocketSession session, CloseStatus status) {
    try {
      return getHandler(session).onClose(session, status);
    }
    finally {
      destroyHandler(session);
    }
  }

  private WebSocketHandler getHandler(WebSocketSession session) {
    WebSocketHandler handler = this.handlers.get(session);
    if (handler == null) {
      throw new IllegalStateException("WebSocketHandler not found for " + session);
    }
    return handler;
  }

  private void destroyHandler(WebSocketSession session) {
    WebSocketHandler handler = this.handlers.remove(session);
    try {
      if (handler != null) {
        this.provider.destroy(handler);
      }
    }
    catch (Throwable ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Error while destroying {}", handler, ex);
      }
    }
  }

  @Override
  public String toString() {
    return "PerConnectionWebSocketHandlerProxy[handlerType=%s]".formatted(this.provider.getHandlerType());
  }

}
