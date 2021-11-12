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

import cn.taketoday.core.task.AsyncListenableTaskExecutor;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.util.concurrent.ListenableFutureTask;
import cn.taketoday.web.socket.StandardEndpoint;
import cn.taketoday.web.socket.StandardWebSocketExtension;
import cn.taketoday.web.socket.StandardWebSocketSession;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.client.AbstractWebSocketClient;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.WebSocketContainer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A WebSocketClient based on standard Java WebSocket API.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketClient extends AbstractWebSocketClient {

  private final WebSocketContainer webSocketContainer;

  private final Map<String, Object> userProperties = new HashMap<>();

  @Nullable
  private AsyncListenableTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


  /**
   * Default constructor that calls {@code ContainerProvider.getWebSocketContainer()}
   * to obtain a (new) {@link WebSocketContainer} instance. Also see constructor
   * accepting existing {@code WebSocketContainer} instance.
   */
  public StandardWebSocketClient() {
    this.webSocketContainer = ContainerProvider.getWebSocketContainer();
  }

  /**
   * Constructor accepting an existing {@link WebSocketContainer} instance.
   * <p>For XML configuration, see {@link WebSocketContainerFactoryBean}. For Java
   * configuration, use {@code ContainerProvider.getWebSocketContainer()} to obtain
   * the {@code WebSocketContainer} instance.
   */
  public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
    Assert.notNull(webSocketContainer, "WebSocketContainer must not be null");
    this.webSocketContainer = webSocketContainer;
  }


  /**
   * The standard Java WebSocket API allows passing "user properties" to the
   * server via {@link ClientEndpointConfig#getUserProperties() userProperties}.
   * Use this property to configure one or more properties to be passed on
   * every handshake.
   */
  public void setUserProperties(@Nullable Map<String, Object> userProperties) {
    if (userProperties != null) {
      this.userProperties.putAll(userProperties);
    }
  }

  /**
   * The configured user properties.
   */
  public Map<String, Object> getUserProperties() {
    return this.userProperties;
  }

  /**
   * Set an {@link AsyncListenableTaskExecutor} to use when opening connections.
   * If this property is set to {@code null}, calls to any of the
   * {@code doHandshake} methods will block until the connection is established.
   * <p>By default, an instance of {@code SimpleAsyncTaskExecutor} is used.
   */
  public void setTaskExecutor(@Nullable AsyncListenableTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Return the configured {@link TaskExecutor}.
   */
  @Nullable
  public AsyncListenableTaskExecutor getTaskExecutor() {
    return this.taskExecutor;
  }

  @Override
  protected ListenableFuture<WebSocketSession> doHandshakeInternal(
          WebSocketHandler webSocketHandler, HttpHeaders headers, URI uri, List<String> protocols,List<WebSocketExtension> extensions) {

    int port = getPort(uri);
    InetSocketAddress localAddress = new InetSocketAddress(getLocalHost(), port);
    InetSocketAddress remoteAddress = new InetSocketAddress(uri.getHost(), port);

    StandardWebSocketSession session = new StandardWebSocketSession(headers, localAddress, remoteAddress);

    ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
            .configurator(new StandardWebSocketClientConfigurator(headers))
            .preferredSubprotocols(protocols)
            .extensions(adaptExtensions(extensions)).build();

    endpointConfig.getUserProperties().putAll(getUserProperties());

    Endpoint endpoint = new StandardEndpoint(session, webSocketHandler);

    Callable<WebSocketSession> connectTask = () -> {
      this.webSocketContainer.connectToServer(endpoint, endpointConfig, uri);
      return session;
    };

    if (this.taskExecutor != null) {
      return this.taskExecutor.submitListenable(connectTask);
    }
    else {
      ListenableFutureTask<WebSocketSession> task = new ListenableFutureTask<>(connectTask);
      task.run();
      return task;
    }
  }

  private static List<Extension> adaptExtensions(List<WebSocketExtension> extensions) {
    ArrayList<Extension> result = new ArrayList<>();
    for (WebSocketExtension extension : extensions) {
      result.add(StandardWebSocketExtension.from(extension));
    }
    return result;
  }

  private InetAddress getLocalHost() {
    try {
      return InetAddress.getLocalHost();
    }
    catch (UnknownHostException ex) {
      return InetAddress.getLoopbackAddress();
    }
  }

  private int getPort(URI uri) {
    if (uri.getPort() == -1) {
      String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
      return "wss".equals(scheme) ? 443 : 80;
    }
    return uri.getPort();
  }

  private class StandardWebSocketClientConfigurator extends Configurator {

    private final HttpHeaders headers;

    public StandardWebSocketClientConfigurator(HttpHeaders headers) {
      this.headers = headers;
    }

    @Override
    public void beforeRequest(Map<String, List<String>> requestHeaders) {
      requestHeaders.putAll(this.headers);
      if (logger.isTraceEnabled()) {
        logger.trace("Handshake request headers: {}", requestHeaders);
      }
    }

    @Override
    public void afterResponse(HandshakeResponse response) {
      if (logger.isTraceEnabled()) {
        logger.trace("Handshake response headers: {}", response.getHeaders());
      }
    }
  }

}
