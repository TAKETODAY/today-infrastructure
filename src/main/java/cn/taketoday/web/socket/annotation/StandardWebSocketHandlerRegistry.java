/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.socket.StandardEndpoint;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHandlerRegistry;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * javax.websocket.*
 *
 * @author TODAY 2021/5/8 22:08
 * @since 3.0.1
 */
public class StandardWebSocketHandlerRegistry extends WebSocketHandlerRegistry {

  @Override
  public void configureParameterResolver(List<ParameterResolver> parameterResolvers) {
    super.configureParameterResolver(parameterResolvers);

    parameterResolvers.add(new RequestContextAttributeParameterResolver(
            WebSocketSession.JAVAX_WEBSOCKET_SESSION_KEY, Session.class));

    parameterResolvers.add(new RequestContextAttributeParameterResolver(
            WebSocketSession.JAVAX_ENDPOINT_CONFIG_KEY, EndpointConfig.class));
  }

  @Override
  protected boolean isOnMessageHandler(Method declaredMethod, BeanDefinition definition) {
    return super.isOnMessageHandler(declaredMethod, definition)
            || declaredMethod.isAnnotationPresent(javax.websocket.OnMessage.class);
  }

  @Override
  protected boolean isOnCloseHandler(Method declaredMethod, BeanDefinition definition) {
    return super.isOnCloseHandler(declaredMethod, definition)
            || declaredMethod.isAnnotationPresent(javax.websocket.OnClose.class);
  }

  @Override
  protected boolean isOnOpenHandler(Method declaredMethod, BeanDefinition definition) {
    return super.isOnOpenHandler(declaredMethod, definition)
            || declaredMethod.isAnnotationPresent(javax.websocket.OnOpen.class);
  }

  @Override
  protected boolean isOnErrorHandler(Method declaredMethod, BeanDefinition definition) {
    return super.isOnErrorHandler(declaredMethod, definition)
            || declaredMethod.isAnnotationPresent(javax.websocket.OnError.class);
  }

  @Override
  protected boolean isEndpoint(BeanDefinition definition) {
    return super.isEndpoint(definition) || definition.isAnnotationPresent(ServerEndpoint.class);
  }

  @Override
  protected WebSocketHandler createWebSocketHandler(BeanDefinition definition,
                                                    WebApplicationContext context,
                                                    AnnotationWebSocketHandler annotationHandler) {
    final StandardAnnotationWebSocketDispatcher socketDispatcher
            = new StandardAnnotationWebSocketDispatcher(annotationHandler, context.getBeanFactory());
    final ServerEndpoint serverEndpoint = definition.getAnnotation(ServerEndpoint.class);
    if (serverEndpoint != null) {
      Configurator configuratorObject = null;
      final Class<? extends Configurator> configurator = serverEndpoint.configurator();
      if (!configurator.equals(Configurator.class)) {
        configuratorObject = ClassUtils.newInstance(configurator);
      }
      ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
              .create(StandardEndpoint.class, serverEndpoint.value())
              .decoders(Arrays.asList(serverEndpoint.decoders()))
              .encoders(Arrays.asList(serverEndpoint.encoders()))
              .subprotocols(Arrays.asList(serverEndpoint.subprotocols()))
              .configurator(configuratorObject)
              .build();

      socketDispatcher.setEndpointConfig(endpointConfig);
    }
    return socketDispatcher;
  }
}
