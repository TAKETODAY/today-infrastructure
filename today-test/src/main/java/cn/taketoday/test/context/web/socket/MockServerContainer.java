/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.context.web.socket;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Mock implementation of the {@link ServerContainer} interface.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class MockServerContainer implements ServerContainer {

  private long defaultAsyncSendTimeout;

  private long defaultMaxSessionIdleTimeout;

  private int defaultMaxBinaryMessageBufferSize;

  private int defaultMaxTextMessageBufferSize;

  // WebSocketContainer

  @Override
  public long getDefaultAsyncSendTimeout() {
    return this.defaultAsyncSendTimeout;
  }

  @Override
  public void setAsyncSendTimeout(long timeout) {
    this.defaultAsyncSendTimeout = timeout;
  }

  @Override
  public long getDefaultMaxSessionIdleTimeout() {
    return this.defaultMaxSessionIdleTimeout;
  }

  @Override
  public void setDefaultMaxSessionIdleTimeout(long timeout) {
    this.defaultMaxSessionIdleTimeout = timeout;
  }

  @Override
  public int getDefaultMaxBinaryMessageBufferSize() {
    return this.defaultMaxBinaryMessageBufferSize;
  }

  @Override
  public void setDefaultMaxBinaryMessageBufferSize(int max) {
    this.defaultMaxBinaryMessageBufferSize = max;
  }

  @Override
  public int getDefaultMaxTextMessageBufferSize() {
    return this.defaultMaxTextMessageBufferSize;
  }

  @Override
  public void setDefaultMaxTextMessageBufferSize(int max) {
    this.defaultMaxTextMessageBufferSize = max;
  }

  @Override
  public Set<Extension> getInstalledExtensions() {
    return Collections.emptySet();
  }

  @Override
  public Session connectToServer(Object annotatedEndpointInstance, URI path) throws DeploymentException, IOException {
    throw new UnsupportedOperationException("MockServerContainer does not support connectToServer(Object, URI)");
  }

  @Override
  public Session connectToServer(Class<?> annotatedEndpointClass, URI path) throws DeploymentException, IOException {
    throw new UnsupportedOperationException("MockServerContainer does not support connectToServer(Class, URI)");
  }

  @Override
  public Session connectToServer(Endpoint endpointInstance, ClientEndpointConfig cec, URI path)
          throws DeploymentException, IOException {

    throw new UnsupportedOperationException(
            "MockServerContainer does not support connectToServer(Endpoint, ClientEndpointConfig, URI)");
  }

  @Override
  public Session connectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec, URI path)
          throws DeploymentException, IOException {

    throw new UnsupportedOperationException(
            "MockServerContainer does not support connectToServer(Class, ClientEndpointConfig, URI)");
  }

  // ServerContainer

  @Override
  public void addEndpoint(Class<?> endpointClass) throws DeploymentException {
    throw new UnsupportedOperationException("MockServerContainer does not support addEndpoint(Class)");
  }

  @Override
  public void addEndpoint(ServerEndpointConfig serverConfig) throws DeploymentException {
    throw new UnsupportedOperationException(
            "MockServerContainer does not support addEndpoint(ServerEndpointConfig)");
  }

}
