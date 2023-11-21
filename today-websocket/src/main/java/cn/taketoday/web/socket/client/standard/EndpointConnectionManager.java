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

package cn.taketoday.web.socket.client.standard;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.client.ConnectionManagerSupport;
import cn.taketoday.web.socket.handler.BeanCreatingHandlerProvider;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

/**
 * A WebSocket connection manager that is given a URI, an {@link Endpoint}, connects to a
 * WebSocket server through the {@link #start()} and {@link #stop()} methods. If
 * {@link #setAutoStartup(boolean)} is set to {@code true} this will be done automatically
 * when the ApplicationContext is refreshed.
 *
 * @author Rossen Stoyanchev
 * @see AnnotatedEndpointConnectionManager
 * @since 4.0
 */
public class EndpointConnectionManager extends ConnectionManagerSupport implements BeanFactoryAware {

  private final Endpoint endpoint;

  @Nullable
  private final BeanCreatingHandlerProvider<Endpoint> endpointProvider;

  private final ClientEndpointConfig.Builder configBuilder = ClientEndpointConfig.Builder.create();

  private WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

  private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("EndpointConnectionManager-");

  @Nullable
  private volatile Session session;

  public EndpointConnectionManager(Endpoint endpoint, String uriTemplate, Object... uriVariables) {
    super(uriTemplate, uriVariables);
    Assert.notNull(endpoint, "endpoint is required");
    this.endpoint = endpoint;
    this.endpointProvider = null;
  }

  public EndpointConnectionManager(Class<? extends Endpoint> endpointClass, String uriTemplate, Object... uriVars) {
    super(uriTemplate, uriVars);
    Assert.notNull(endpointClass, "endpointClass is required");
    this.endpoint = null;
    this.endpointProvider = new BeanCreatingHandlerProvider<>(endpointClass);
  }

  public void setSupportedProtocols(String... protocols) {
    this.configBuilder.preferredSubprotocols(Arrays.asList(protocols));
  }

  public void setExtensions(Extension... extensions) {
    this.configBuilder.extensions(Arrays.asList(extensions));
  }

  public void setEncoders(List<Class<? extends Encoder>> encoders) {
    this.configBuilder.encoders(encoders);
  }

  public void setDecoders(List<Class<? extends Decoder>> decoders) {
    this.configBuilder.decoders(decoders);
  }

  public void setConfigurator(Configurator configurator) {
    this.configBuilder.configurator(configurator);
  }

  public void setWebSocketContainer(WebSocketContainer webSocketContainer) {
    this.webSocketContainer = webSocketContainer;
  }

  public WebSocketContainer getWebSocketContainer() {
    return this.webSocketContainer;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (this.endpointProvider != null) {
      this.endpointProvider.setBeanFactory(beanFactory);
    }
  }

  /**
   * Set a {@link TaskExecutor} to use to open connections.
   * By default {@link SimpleAsyncTaskExecutor} is used.
   */
  public void setTaskExecutor(TaskExecutor taskExecutor) {
    Assert.notNull(taskExecutor, "TaskExecutor is required");
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
        if (logger.isInfoEnabled()) {
          logger.info("Connecting to WebSocket at {}", getUri());
        }
        Endpoint endpointToUse = this.endpoint;
        if (endpointToUse == null) {
          Assert.state(this.endpointProvider != null, "No endpoint set");
          endpointToUse = this.endpointProvider.getHandler();
        }
        ClientEndpointConfig endpointConfig = this.configBuilder.build();
        this.session = getWebSocketContainer().connectToServer(endpointToUse, endpointConfig, getUri());
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
