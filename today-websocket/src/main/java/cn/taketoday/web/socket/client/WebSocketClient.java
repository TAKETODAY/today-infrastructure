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
package cn.taketoday.web.socket.client;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHttpHeaders;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * Contract for initiating a WebSocket request. As an alternative considering using the
 * declarative style {@link WebSocketConnectionManager} that starts a WebSocket connection
 * to a pre-configured URI when the application starts.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/12 15:58
 * @see WebSocketConnectionManager
 * @since 4.0
 */
public interface WebSocketClient {

  ListenableFuture<WebSocketSession> doHandshake(
          WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVariables);

  ListenableFuture<WebSocketSession> doHandshake(
          WebSocketHandler webSocketHandler, @Nullable WebSocketHttpHeaders headers, URI uri);

  /**
   * Execute a handshake request to the given url and handle the resulting
   * WebSocket session with the given handler.
   *
   * @param webSocketHandler the session handler
   * @param uriTemplate the url template
   * @param uriVariables the variables to expand the template
   * @return a future that completes when the session is available
   */
  CompletableFuture<WebSocketSession> execute(WebSocketHandler webSocketHandler,
          String uriTemplate, Object... uriVariables);

  /**
   * Execute a handshake request to the given url and handle the resulting
   * WebSocket session with the given handler.
   *
   * @param webSocketHandler the session handler
   * @param uri the url
   * @return a future that completes when the session is available
   */
  CompletableFuture<WebSocketSession> execute(WebSocketHandler webSocketHandler,
          @Nullable WebSocketHttpHeaders headers, URI uri);

}
