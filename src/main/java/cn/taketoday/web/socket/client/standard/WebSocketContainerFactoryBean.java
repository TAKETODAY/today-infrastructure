/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import cn.taketoday.beans.factory.FactoryBean;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;

/**
 * A FactoryBean for creating and configuring a {@link WebSocketContainer}
 * through XML configuration. In Java configuration, ignore this class and use
 * {@code ContainerProvider.getWebSocketContainer()} instead.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketContainerFactoryBean implements FactoryBean<WebSocketContainer> {

  private final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

  public void setAsyncSendTimeout(long timeoutInMillis) {
    this.webSocketContainer.setAsyncSendTimeout(timeoutInMillis);
  }

  public long getAsyncSendTimeout() {
    return this.webSocketContainer.getDefaultAsyncSendTimeout();
  }

  public void setMaxSessionIdleTimeout(long timeoutInMillis) {
    this.webSocketContainer.setDefaultMaxSessionIdleTimeout(timeoutInMillis);
  }

  public long getMaxSessionIdleTimeout() {
    return this.webSocketContainer.getDefaultMaxSessionIdleTimeout();
  }

  public void setMaxTextMessageBufferSize(int bufferSize) {
    this.webSocketContainer.setDefaultMaxTextMessageBufferSize(bufferSize);
  }

  public int getMaxTextMessageBufferSize() {
    return this.webSocketContainer.getDefaultMaxTextMessageBufferSize();
  }

  public void setMaxBinaryMessageBufferSize(int bufferSize) {
    this.webSocketContainer.setDefaultMaxBinaryMessageBufferSize(bufferSize);
  }

  public int getMaxBinaryMessageBufferSize() {
    return this.webSocketContainer.getDefaultMaxBinaryMessageBufferSize();
  }

  @Override
  public WebSocketContainer getObject() {
    return this.webSocketContainer;
  }

  @Override
  public Class<?> getObjectType() {
    return WebSocketContainer.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
