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

package cn.taketoday.web.socket.config;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.util.CollectionUtils;

/**
 * A variation of {@link WebSocketConfigurationSupport} that detects implementations of
 * {@link WebSocketConfigurer} in Infra configuration and invokes them in order to
 * configure WebSocket request handling.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
public class DelegatingWebSocketConfiguration extends WebSocketConfigurationSupport {

  private final List<WebSocketConfigurer> configurers = new ArrayList<>();

  @Autowired(required = false)
  public void setConfigurers(List<WebSocketConfigurer> configurers) {
    if (CollectionUtils.isNotEmpty(configurers)) {
      this.configurers.addAll(configurers);
    }
  }

  @Override
  protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    for (WebSocketConfigurer configurer : this.configurers) {
      configurer.registerWebSocketHandlers(registry);
    }
  }

}
