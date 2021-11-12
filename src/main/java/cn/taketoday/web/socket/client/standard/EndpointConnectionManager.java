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
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.BeanCreatingHandlerProvider;
import cn.taketoday.web.socket.client.ConnectionManagerSupport;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.util.Arrays;
import java.util.List;

/**
 * A WebSocket connection manager that is given a URI, an {@link Endpoint}, connects to a
 * WebSocket server through the {@link #start()} and {@link #stop()} methods. If
 * {@link #setAutoStartup(boolean)} is set to {@code true} this will be done automatically
 * when the Spring ApplicationContext is refreshed.
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
    Assert.notNull(endpoint, "endpoint must not be null");
    this.endpoint = endpoint;
    this.endpointProvider = null;
  }

  public EndpointConnectionManager(Class<? extends Endpoint> endpointClass, String uriTemplate, Object... uriVars) {
    super(uriTemplate, uriVars);
    Assert.notNull(endpointClass, "endpointClass must not be null");
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
