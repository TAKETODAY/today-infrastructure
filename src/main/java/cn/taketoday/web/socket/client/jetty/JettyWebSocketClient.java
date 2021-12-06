/*
 * Copyright 2002-2021 the original author or authors.
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

package cn.taketoday.web.socket.client.jetty;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.Lifecycle;
import cn.taketoday.core.task.AsyncListenableTaskExecutor;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.util.concurrent.ListenableFutureTask;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.client.AbstractWebSocketClient;
import cn.taketoday.web.socket.jetty.JettyWebSocketHandler;
import cn.taketoday.web.socket.jetty.JettyWebSocketSession;
import cn.taketoday.web.socket.jetty.WebSocketToJettyExtensionConfigAdapter;
import cn.taketoday.web.util.UriComponents;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * Initiates WebSocket requests to a WebSocket server programmatically
 * through the Jetty WebSocket API.
 *
 * <p>As of 4.1 this class implements {@link Lifecycle} rather than
 * {@link cn.taketoday.context.SmartLifecycle}. Use
 * {@link cn.taketoday.web.socket.client.WebSocketConnectionManager
 * WebSocketConnectionManager} instead to auto-start a WebSocket connection.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class JettyWebSocketClient extends AbstractWebSocketClient implements Lifecycle {

  private final WebSocketClient client;

  @Nullable
  private AsyncListenableTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

  /**
   * Default constructor that creates an instance of
   * {@link WebSocketClient}.
   */
  public JettyWebSocketClient() {
    this.client = new WebSocketClient();
  }

  /**
   * Constructor that accepts an existing
   * {@link WebSocketClient} instance.
   */
  public JettyWebSocketClient(WebSocketClient client) {
    this.client = client;
  }

  /**
   * Set an {@link AsyncListenableTaskExecutor} to use when opening connections.
   * If this property is set to {@code null}, calls to any of the
   * {@code doHandshake} methods will block until the connection is established.
   * <p>By default an instance of {@code SimpleAsyncTaskExecutor} is used.
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
  public void start() {
    try {
      this.client.start();
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to start Jetty WebSocketClient", ex);
    }
  }

  @Override
  public void stop() {
    try {
      this.client.stop();
    }
    catch (Exception ex) {
      logger.error("Failed to stop Jetty WebSocketClient", ex);
    }
  }

  @Override
  public boolean isRunning() {
    return this.client.isStarted();
  }

  @Override
  public ListenableFuture<WebSocketSession> doHandshake(
          WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVars) {

    UriComponents uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode();
    return doHandshake(webSocketHandler, null, uriComponents.toUri());
  }

  @Override
  public ListenableFuture<WebSocketSession> doHandshakeInternal(
          WebSocketHandler wsHandler, HttpHeaders headers, URI uri, List<String> protocols, List<WebSocketExtension> extensions) {

    ClientUpgradeRequest request = new ClientUpgradeRequest();
    request.setSubProtocols(protocols);

    for (WebSocketExtension extension : extensions) {
      request.addExtensions(new WebSocketToJettyExtensionConfigAdapter(extension));
    }

    request.setHeaders(headers);

    JettyWebSocketSession wsSession = new JettyWebSocketSession();

    Callable<WebSocketSession> connectTask = () -> {
      JettyWebSocketHandler adapter = new JettyWebSocketHandler(wsHandler, wsSession);
      Future<Session> future = this.client.connect(adapter, uri, request);
      future.get(this.client.getConnectTimeout() + 2000, TimeUnit.MILLISECONDS);
      return wsSession;
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

}
