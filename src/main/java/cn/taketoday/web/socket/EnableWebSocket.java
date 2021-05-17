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

package cn.taketoday.web.socket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.ObjectNotationProcessor;
import cn.taketoday.web.socket.annotation.AnnotationWebSocketHandlerBuilder;
import cn.taketoday.web.socket.annotation.EndpointParameterResolver;
import cn.taketoday.web.socket.annotation.MessageBodyEndpointParameterResolver;
import cn.taketoday.web.socket.annotation.StandardAnnotationWebSocketHandlerBuilder;
import cn.taketoday.web.socket.annotation.StandardWebSocketHandlerRegistry;
import cn.taketoday.web.socket.annotation.WebSocketSessionParameterResolver;
import cn.taketoday.web.socket.tomcat.TomcatWebSocketHandlerAdapter;

/**
 * @author TODAY 2021/4/5 12:14
 * @since 3.0
 */
@Import(WebSocketConfig.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableWebSocket {

}

class WebSocketConfig implements WebApplicationInitializer {

  @MissingBean(type = AbstractWebSocketHandlerAdapter.class)
  TomcatWebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new TomcatWebSocketHandlerAdapter();
  }

  @MissingBean
  WebSocketHandlerRegistry webSocketHandlerRegistry(AnnotationWebSocketHandlerBuilder handlerBuilder) {
    if (ClassUtils.isPresent("javax.websocket.Session")) {
      return new StandardWebSocketHandlerRegistry(handlerBuilder);
    }
    return new WebSocketHandlerRegistry(handlerBuilder);
  }

  @MissingBean
  WebSocketSessionParameterResolver webSocketSessionParameterResolver() {
    return new WebSocketSessionParameterResolver();
  }

  @MissingBean(type = AnnotationWebSocketHandlerBuilder.class)
  AnnotationWebSocketHandlerBuilder annotationWebSocketHandlerBuilder(List<EndpointParameterResolver> resolvers) {
    final AnnotationWebSocketHandlerBuilder handlerBuilder;
    if (ClassUtils.isPresent("javax.websocket.Session")) {
      handlerBuilder = new StandardAnnotationWebSocketHandlerBuilder();
    }
    else {
      handlerBuilder = new AnnotationWebSocketHandlerBuilder();
    }
    handlerBuilder.registerDefaultResolvers();
    handlerBuilder.addResolvers(resolvers);
    return handlerBuilder;
  }

  @MissingBean
  MessageBodyEndpointParameterResolver messageBodyEndpointParameterResolver(ObjectNotationProcessor notationProcessor) {
    return new MessageBodyEndpointParameterResolver(notationProcessor);
  }

  @Override
  public void onStartup(WebApplicationContext context) throws Throwable {
    WebSocketHandlerRegistry handlerRegistry = context.getBean(WebSocketHandlerRegistry.class);
    List<WebSocketConfiguration> configurers = context.getBeans(WebSocketConfiguration.class);
    List<EndpointParameterResolver> resolvers = context.getBeans(EndpointParameterResolver.class);

    // configure WebSocketHandlers
    for (final WebSocketConfiguration configurer : configurers) {
      configurer.configureWebSocketHandlers(handlerRegistry);
    }

    // configure EndpointParameterResolver
    for (final WebSocketConfiguration configurer : configurers) {
      configurer.configureEndpointParameterResolvers(resolvers);
    }

  }
}
