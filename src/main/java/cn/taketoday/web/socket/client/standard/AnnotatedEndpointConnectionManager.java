/*
 * Copyright 2002-2018 the original author or authors.
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

package cn.taketoday.web.socket.client.standard;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.BeanCreatingHandlerProvider;
import cn.taketoday.web.socket.client.ConnectionManagerSupport;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

/**
 * A WebSocket connection manager that is given a URI, a
 * {@link jakarta.websocket.ClientEndpoint}-annotated endpoint, connects to a
 * WebSocket server through the {@link #start()} and {@link #stop()} methods.
 * If {@link #setAutoStartup(boolean)} is set to {@code true} this will be
 * done automatically when the Spring ApplicationContext is refreshed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotatedEndpointConnectionManager extends ConnectionManagerSupport implements BeanFactoryAware {

  @Nullable
  private final Object endpoint;

  @Nullable
  private final BeanCreatingHandlerProvider<Object> endpointProvider;

  private WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

  private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("AnnotatedEndpointConnectionManager-");

  @Nullable
  private volatile Session session;

  public AnnotatedEndpointConnectionManager(@Nullable Object endpoint, String uriTemplate, Object... uriVariables) {
    super(uriTemplate, uriVariables);
    this.endpoint = endpoint;
    this.endpointProvider = null;
  }

  public AnnotatedEndpointConnectionManager(Class<?> endpointClass, String uriTemplate, Object... uriVariables) {
    super(uriTemplate, uriVariables);
    this.endpoint = null;
    this.endpointProvider = new BeanCreatingHandlerProvider<>(endpointClass);
  }

  public void setWebSocketContainer(WebSocketContainer webSocketContainer) {
    this.webSocketContainer = webSocketContainer;
  }

  public WebSocketContainer getWebSocketContainer() {
    return this.webSocketContainer;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (this.endpointProvider != null) {
      this.endpointProvider.setBeanFactory(beanFactory);
    }
  }

  /**
   * Set a {@link TaskExecutor} to use to open the connection.
   * By default {@link SimpleAsyncTaskExecutor} is used.
   */
  public void setTaskExecutor(TaskExecutor taskExecutor) {
    Assert.notNull(taskExecutor, "TaskExecutor must not be null");
    this.taskExecutor = taskExecutor;
  }

  /**
   * Return the configured {@link TaskExecutor}.
   */
  public TaskExecutor getTaskExecutor() {
    return this.taskExecutor;
  }

  @Override
  protected void openConnection() {
    this.taskExecutor.execute(() -> {
      try {
        logger.info("Connecting to WebSocket at {}", getUri());
        Object endpointToUse = this.endpoint;
        if (endpointToUse == null) {
          Assert.state(this.endpointProvider != null, "No endpoint set");
          endpointToUse = this.endpointProvider.getHandler();
        }
        this.session = this.webSocketContainer.connectToServer(endpointToUse, getUri());
        logger.info("Successfully connected to WebSocket");
      }
      catch (Throwable ex) {
        logger.error("Failed to connect to WebSocket", ex);
      }
    });
  }

  @Override
  protected void closeConnection() throws Exception {
    try {
      Session session = this.session;
      if (session != null && session.isOpen()) {
        session.close();
      }
    }
    finally {
      this.session = null;
    }
  }

  @Override
  protected boolean isConnected() {
    Session session = this.session;
    return (session != null && session.isOpen());
  }

}
