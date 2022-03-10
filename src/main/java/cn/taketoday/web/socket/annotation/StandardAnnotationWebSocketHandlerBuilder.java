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

package cn.taketoday.web.socket.annotation;

import java.util.Arrays;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.socket.StandardEndpoint;
import cn.taketoday.web.socket.WebSocketHandler;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * @author TODAY 2021/5/12 23:39
 * @since 3.0.1
 */
public class StandardAnnotationWebSocketHandlerBuilder extends AnnotationWebSocketHandlerBuilder {

  @Override
  public void registerDefaultResolvers() {
    super.registerDefaultResolvers();

    resolvers.add(new PathParamEndpointParameterResolver(getConversionService()));
    resolvers.add(new EndpointConfigEndpointParameterResolver());
    resolvers.add(new StandardSessionEndpointParameterResolver());
  }

  @Override
  public WebSocketHandler build(
          String beanName, BeanDefinition definition, WebApplicationContext context, WebSocketHandlerDelegate annotationHandler) {
    StandardAnnotationWebSocketDispatcher socketDispatcher
            = new StandardAnnotationWebSocketDispatcher(annotationHandler, resolvers, supportPartialMessage);

    MergedAnnotation<ServerEndpoint> serverEndpoint = context.findAnnotationOnBean(beanName, ServerEndpoint.class);
    if (serverEndpoint.isPresent()) {
      ServerEndpointConfig.Configurator configuratorObject = null;
      Class<? extends ServerEndpointConfig.Configurator> configurator = serverEndpoint.getClass("configurator");
      if (!configurator.equals(ServerEndpointConfig.Configurator.class)) {
        configuratorObject = BeanUtils.newInstance(configurator);
      }
      ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
              .create(StandardEndpoint.class, serverEndpoint.getStringValue())
              .decoders(Arrays.asList(serverEndpoint.getClassArray("decoders")))
              .encoders(Arrays.asList(serverEndpoint.getClassArray("encoders")))
              .subprotocols(Arrays.asList(serverEndpoint.getStringArray("subprotocols")))
              .configurator(configuratorObject)
              .build();

      socketDispatcher.setEndpointConfig(endpointConfig);
    }
    return socketDispatcher;
  }

}
