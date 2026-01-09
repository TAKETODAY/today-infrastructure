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

package infra.web.socket.client;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;

import infra.context.Lifecycle;
import infra.http.HttpHeaders;
import infra.util.concurrent.Future;
import infra.util.concurrent.FutureListener;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketHttpHeaders;
import infra.web.socket.WebSocketSession;
import infra.web.socket.handler.LoggingWebSocketHandler;

/**
 * A WebSocket connection manager that is given a URI, a {@link WebSocketClient}, and a
 * {@link WebSocketHandler}, connects to a WebSocket server through {@link #start()} and
 * {@link #stop()} methods. If {@link #setAutoStartup(boolean)} is set to {@code true}
 * this will be done automatically when the ApplicationContext is refreshed.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/12 15:58
 */
public class WebSocketConnectionManager extends ConnectionManagerSupport implements FutureListener<Future<WebSocketSession>> {

  private final WebSocketClient client;

  private final WebSocketHandler webSocketHandler;

  @Nullable
  private WebSocketSession webSocketSession;

  private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

  /**
   * Constructor with the client to use and a handler to handle messages with.
   */
  public WebSocketConnectionManager(WebSocketClient client, WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVariables) {
    super(uriTemplate, uriVariables);
    this.client = client;
    this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
  }

  /**
   * Variant of {@link #WebSocketConnectionManager(WebSocketClient, WebSocketHandler, String, Object...)}
   * with a prepared {@link URI}.
   */
  public WebSocketConnectionManager(WebSocketClient client, WebSocketHandler webSocketHandler, URI uri) {
    super(uri);
    this.client = client;
    this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
  }

  /**
   * Decorate the WebSocketHandler provided to the class constructor.
   * <p>By default {@link LoggingWebSocketHandler} is added.
   */
  protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
    return new LoggingWebSocketHandler(handler);
  }

  /**
   * Set the sub-protocols to use. If configured, specified sub-protocols will be
   * requested in the handshake through the {@code Sec-WebSocket-Protocol} header. The
   * resulting WebSocket session will contain the protocol accepted by the server, if
   * any.
   */
  public void setSubProtocols(List<String> protocols) {
    this.headers.setSecWebSocketProtocol(protocols);
  }

  /**
   * Return the configured sub-protocols to use.
   */
  public List<String> getSubProtocols() {
    return this.headers.getSecWebSocketProtocol();
  }

  /**
   * Set the origin to use.
   */
  public void setOrigin(@Nullable String origin) {
    this.headers.setOrigin(origin);
  }

  /**
   * Return the configured origin.
   */
  @Nullable
  public String getOrigin() {
    return this.headers.getOrigin();
  }

  /**
   * Provide default headers to add to the WebSocket handshake request.
   */
  public void setHeaders(HttpHeaders headers) {
    this.headers.clear();
    this.headers.putAll(headers);
  }

  /**
   * Return the default headers for the WebSocket handshake request.
   */
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public void startInternal() {
    if (this.client instanceof Lifecycle lifecycle && !lifecycle.isRunning()) {
      lifecycle.start();
    }
    super.startInternal();
  }

  @Override
  public void stopInternal() throws Exception {
    if (this.client instanceof Lifecycle lifecycle && lifecycle.isRunning()) {
      lifecycle.stop();
    }
    super.stopInternal();
  }

  @Override
  protected void openConnection() {
    logger.info("Connecting to WebSocket at {}", getUri());
    client.connect(getUri(), this.headers, webSocketHandler)
            .onCompleted(this);
  }

  @Override
  protected void closeConnection() throws Exception {
    if (this.webSocketSession != null) {
      this.webSocketSession.close();
    }
  }

  @Override
  protected boolean isConnected() {
    return (this.webSocketSession != null && this.webSocketSession.isOpen());
  }

  @Override
  public void operationComplete(Future<WebSocketSession> future) {
    if (future.isSuccess()) {
      webSocketSession = future.getNow();
      logger.info("Successfully connected");
    }
    else {
      logger.error("Failed to connect", future.getCause());
    }
  }

}
