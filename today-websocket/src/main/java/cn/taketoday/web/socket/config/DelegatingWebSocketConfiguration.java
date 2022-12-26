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

package cn.taketoday.web.socket.config;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.socket.annotation.EndpointParameterResolver;

/**
 * A variation of {@link WebSocketConfigurationSupport} that detects implementations of
 * {@link WebSocketConfigurer} in Spring configuration and invokes them in order to
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
    if (!CollectionUtils.isEmpty(configurers)) {
      this.configurers.addAll(configurers);
    }
  }

  @Override
  protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    for (WebSocketConfigurer configurer : this.configurers) {
      configurer.registerWebSocketHandlers(registry);
    }
  }

  @Override
  protected void registerEndpointParameterStrategies(List<EndpointParameterResolver> resolvers) {
    for (WebSocketConfigurer configurer : configurers) {
      configurer.registerEndpointParameterStrategies(resolvers);
    }
  }
}
