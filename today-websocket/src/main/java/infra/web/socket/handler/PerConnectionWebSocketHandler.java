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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
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
