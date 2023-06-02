/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket.client.standard;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.client.ConnectionManagerSupport;
import cn.taketoday.web.socket.handler.BeanCreatingHandlerProvider;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

/**
 * A WebSocket connection manager that is given a URI, a
 * {@link jakarta.websocket.ClientEndpoint}-annotated endpoint, connects to a
 * WebSocket server through the {@link #start()} and {@link #stop()} methods.
 * If {@link #setAutoStartup(boolean)} is set to {@code true} this will be
 * done automatically when the ApplicationContext is refreshed.
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
