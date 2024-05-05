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

package cn.taketoday.web.socket.server.standard;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.mock.ServletContextAware;
import cn.taketoday.mock.api.ServletContext;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerContainer;

/**
 * A {@link FactoryBean} for configuring {@link ServerContainer}.
 * Since there is usually only one {@code ServerContainer} instance accessible under a
 * well-known {@code cn.taketoday.mock.api.ServletContext} attribute, simply declaring this
 * FactoryBean and using its setters allows for configuring the {@code ServerContainer}
 * through Infra configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ServletServerContainerFactoryBean
        implements FactoryBean<WebSocketContainer>, ServletContextAware, InitializingBean {

  @Nullable
  private Long asyncSendTimeout;

  @Nullable
  private Long maxSessionIdleTimeout;

  @Nullable
  private Integer maxTextMessageBufferSize;

  @Nullable
  private Integer maxBinaryMessageBufferSize;

  @Nullable
  private ServletContext servletContext;

  @Nullable
  private ServerContainer serverContainer;

  public void setAsyncSendTimeout(Long timeoutInMillis) {
    this.asyncSendTimeout = timeoutInMillis;
  }

  @Nullable
  public Long getAsyncSendTimeout() {
    return this.asyncSendTimeout;
  }

  public void setMaxSessionIdleTimeout(Long timeoutInMillis) {
    this.maxSessionIdleTimeout = timeoutInMillis;
  }

  @Nullable
  public Long getMaxSessionIdleTimeout() {
    return this.maxSessionIdleTimeout;
  }

  public void setMaxTextMessageBufferSize(Integer bufferSize) {
    this.maxTextMessageBufferSize = bufferSize;
  }

  @Nullable
  public Integer getMaxTextMessageBufferSize() {
    return this.maxTextMessageBufferSize;
  }

  public void setMaxBinaryMessageBufferSize(Integer bufferSize) {
    this.maxBinaryMessageBufferSize = bufferSize;
  }

  @Nullable
  public Integer getMaxBinaryMessageBufferSize() {
    return this.maxBinaryMessageBufferSize;
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Override
  public void afterPropertiesSet() {
    Assert.state(this.servletContext != null,
            "A ServletContext is required to access the jakarta.websocket.server.ServerContainer instance");
    this.serverContainer = (ServerContainer) this.servletContext.getAttribute(
            "jakarta.websocket.server.ServerContainer");
    Assert.state(this.serverContainer != null,
            "Attribute 'jakarta.websocket.server.ServerContainer' not found in ServletContext");

    if (this.asyncSendTimeout != null) {
      this.serverContainer.setAsyncSendTimeout(this.asyncSendTimeout);
    }
    if (this.maxSessionIdleTimeout != null) {
      this.serverContainer.setDefaultMaxSessionIdleTimeout(this.maxSessionIdleTimeout);
    }
    if (this.maxTextMessageBufferSize != null) {
      this.serverContainer.setDefaultMaxTextMessageBufferSize(this.maxTextMessageBufferSize);
    }
    if (this.maxBinaryMessageBufferSize != null) {
      this.serverContainer.setDefaultMaxBinaryMessageBufferSize(this.maxBinaryMessageBufferSize);
    }
  }

  @Override
  @Nullable
  public ServerContainer getObject() {
    return this.serverContainer;
  }

  @Override
  public Class<?> getObjectType() {
    return (this.serverContainer != null ? this.serverContainer.getClass() : ServerContainer.class);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
