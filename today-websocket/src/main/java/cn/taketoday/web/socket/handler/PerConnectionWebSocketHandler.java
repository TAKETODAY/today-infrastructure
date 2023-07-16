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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;

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
 * declaring this class as a Infra bean will do that. Otherwise, {@link WebSocketHandler}
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

  private final boolean supportsPartialMessages;

  public PerConnectionWebSocketHandler(Class<? extends WebSocketHandler> handlerType) {
    this(handlerType, false);
  }

  public PerConnectionWebSocketHandler(Class<? extends WebSocketHandler> handlerType, boolean supportsPartialMessages) {
    this.provider = new BeanCreatingHandlerProvider<>(handlerType);
    this.supportsPartialMessages = supportsPartialMessages;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.provider.setBeanFactory(beanFactory);
  }

  @Override
  public void onOpen(WebSocketSession session) throws Exception {
    WebSocketHandler handler = this.provider.getHandler();
    this.handlers.put(session, handler);
    handler.onOpen(session);
  }

  @Override
  public void handleMessage(WebSocketSession session, Message<?> message) throws Exception {
    getHandler(session).handleMessage(session, message);
  }

  @Override
  public void onError(WebSocketSession session, Throwable exception) throws Exception {
    getHandler(session).onError(session, exception);
  }

  @Override
  public void onClose(WebSocketSession session, CloseStatus status) throws Exception {
    try {
      getHandler(session).onClose(session, status);
    }
    finally {
      destroyHandler(session);
    }
  }

  @Override
  public boolean supportPartialMessage() {
    return this.supportsPartialMessages;
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
        logger.warn("Error while destroying " + handler, ex);
      }
    }
  }

  @Override
  public String toString() {
    return "PerConnectionWebSocketHandlerProxy[handlerType=" + this.provider.getHandlerType() + "]";
  }

}
