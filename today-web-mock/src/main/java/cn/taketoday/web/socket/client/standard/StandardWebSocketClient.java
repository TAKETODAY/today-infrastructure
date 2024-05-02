/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.socket.client.standard;

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
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;

import cn.taketoday.core.Decorator;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.util.concurrent.FutureUtils;
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

/**
 * A WebSocketClient based on standard Java WebSocket API.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandardWebSocketClient extends AbstractWebSocketClient {

  private final WebSocketContainer webSocketContainer;

  private final HashMap<String, Object> userProperties = new HashMap<>();

  @Nullable
  private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

  @Nullable
  private Decorator<WebSocketSession> sessionDecorator;

  @Nullable
  private SSLContext sslContext;

  /**
   * Default constructor that calls {@code ContainerProvider.getWebSocketContainer()}
   * to obtain a (new) {@link WebSocketContainer} instance. Also see constructor
   * accepting existing {@code WebSocketContainer} instance.
   */
  public StandardWebSocketClient() {
    this(ContainerProvider.getWebSocketContainer());
  }

  /**
   * Constructor accepting an existing {@link WebSocketContainer} instance.
   * <p>For XML configuration, see {@link WebSocketContainerFactoryBean}. For Java
   * configuration, use {@code ContainerProvider.getWebSocketContainer()} to obtain
   * the {@code WebSocketContainer} instance.
   */
  public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
    Assert.notNull(webSocketContainer, "WebSocketContainer is required");
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
   * Set an {@link AsyncTaskExecutor} to use when opening connections.
   * If this property is set to {@code null}, calls to any of the
   * {@code doHandshake} methods will block until the connection is established.
   * <p>By default, an instance of {@code SimpleAsyncTaskExecutor} is used.
   */
  public void setTaskExecutor(@Nullable AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Return the configured {@link TaskExecutor}.
   */
  @Nullable
  public AsyncTaskExecutor getTaskExecutor() {
    return this.taskExecutor;
  }

  /**
   * Set the {@link SSLContext} to use for {@link ClientEndpointConfig#getSSLContext()}.
   */
  public void setSslContext(@Nullable SSLContext sslContext) {
    this.sslContext = sslContext;
  }

  /**
   * Return the {@link SSLContext} to use.
   */
  @Nullable
  public SSLContext getSslContext() {
    return this.sslContext;
  }

  public void setSessionDecorator(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    this.sessionDecorator = sessionDecorator;
  }

  public void addSessionDecorator(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    if (sessionDecorator != null) {
      if (this.sessionDecorator != null) {
        this.sessionDecorator = this.sessionDecorator.andThen(sessionDecorator);
      }
      else {
        this.sessionDecorator = sessionDecorator;
      }
    }
  }

  @Override
  protected Future<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
          HttpHeaders headers, URI uri, List<String> protocols, List<WebSocketExtension> extensions) {

    int port = getPort(uri);
    InetSocketAddress localAddress = new InetSocketAddress(getLocalHost(), port);
    InetSocketAddress remoteAddress = new InetSocketAddress(uri.getHost(), port);

    WebSocketSession session = createSession(headers, localAddress, remoteAddress);

    ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
            .configurator(new StandardWebSocketClientConfigurator(headers))
            .preferredSubprotocols(protocols)
            .extensions(adaptExtensions(extensions))
            .sslContext(getSslContext())
            .build();

    endpointConfig.getUserProperties().putAll(getUserProperties());

    Endpoint endpoint = new StandardEndpoint(session, webSocketHandler);

    Callable<WebSocketSession> connectTask = () -> {
      this.webSocketContainer.connectToServer(endpoint, endpointConfig, uri);
      return session;
    };

    if (this.taskExecutor != null) {
      return this.taskExecutor.submit(connectTask);
    }
    else {
      var task = Future.forFutureTask(connectTask);
      task.run();
      return task;
    }
  }

  private WebSocketSession createSession(HttpHeaders headers, InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
    WebSocketSession session = new StandardWebSocketSession(headers, localAddress, remoteAddress);
    if (sessionDecorator != null) {
      session = sessionDecorator.decorate(session);
    }
    return session;
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
