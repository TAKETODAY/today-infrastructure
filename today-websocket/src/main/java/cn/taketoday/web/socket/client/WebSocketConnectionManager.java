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

package cn.taketoday.web.socket.client;

import java.net.URI;
import java.util.List;

import cn.taketoday.context.Lifecycle;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.concurrent.FutureListener;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHttpHeaders;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.handler.LoggingWebSocketHandlerDecorator;

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
   * <p>By default {@link LoggingWebSocketHandlerDecorator} is added.
   */
  protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
    return new LoggingWebSocketHandlerDecorator(handler);
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
    client.connect(this.webSocketHandler, this.headers, getUri())
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
