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

package cn.taketoday.web.socket;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.lang.NonNull;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.socket.annotation.AnnotationWebSocketHandlerBuilder;
import cn.taketoday.web.socket.annotation.EndpointParameterResolver;
import cn.taketoday.web.socket.annotation.MessageBodyEndpointParameterResolver;
import cn.taketoday.web.socket.annotation.StandardAnnotationWebSocketHandlerBuilder;
import cn.taketoday.web.socket.annotation.StandardWebSocketHandlerRegistry;
import cn.taketoday.web.socket.annotation.WebSocketSessionParameterResolver;
import cn.taketoday.web.socket.jetty.JettyWebSocketHandlerAdapter;
import cn.taketoday.web.socket.tomcat.TomcatWebSocketHandlerAdapter;
import cn.taketoday.web.socket.undertow.UndertowWebSocketHandlerAdapter;

/**
 * @author TODAY 2021/4/5 12:14
 * @since 3.0
 */
@Import(WebSocketConfig.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableWebSocket {

}

@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
class WebSocketConfig {

  @MissingBean(AbstractWebSocketHandlerAdapter.class)
  AbstractWebSocketHandlerAdapter webSocketHandlerAdapter() {
    if (ClassUtils.isPresent("org.apache.tomcat.websocket.WsHandshakeResponse")) {
      return new TomcatWebSocketHandlerAdapter();
    }
    if (ClassUtils.isPresent("io.undertow.websockets.jsr.handshake.HandshakeUtil")) {
      return new UndertowWebSocketHandlerAdapter();
    }
    if (ClassUtils.isPresent("org.eclipse.jetty.websocket.servlet.WebSocketCreator")) {
      return new JettyWebSocketHandlerAdapter();
    }
    throw new IllegalStateException("No AbstractWebSocketHandlerAdapter");
  }

  @MissingBean
  WebSocketHandlerRegistry webSocketHandlerRegistry(
          List<WebSocketConfiguration> configurers, AnnotationWebSocketHandlerBuilder handlerBuilder) {
    WebSocketHandlerRegistry registry = getRegistry(handlerBuilder);

    // configure WebSocketHandlers
    for (WebSocketConfiguration configurer : configurers) {
      configurer.configureWebSocketHandlers(registry);
    }

    return registry;
  }

  @NonNull
  private WebSocketHandlerRegistry getRegistry(AnnotationWebSocketHandlerBuilder handlerBuilder) {
    if (ClassUtils.isPresent("jakarta.websocket.Session")) {
      return new StandardWebSocketHandlerRegistry(handlerBuilder);
    }
    return new WebSocketHandlerRegistry(handlerBuilder);
  }

  @MissingBean
  WebSocketSessionParameterResolver webSocketSessionParameterResolver() {
    return new WebSocketSessionParameterResolver();
  }

  @MissingBean(value = AnnotationWebSocketHandlerBuilder.class)
  AnnotationWebSocketHandlerBuilder annotationWebSocketHandlerBuilder(
          List<EndpointParameterResolver> resolvers, List<WebSocketConfiguration> configurers) {
    AnnotationWebSocketHandlerBuilder handlerBuilder;

    if (ClassUtils.isPresent("jakarta.websocket.Session")) {
      handlerBuilder = new StandardAnnotationWebSocketHandlerBuilder();
    }
    else {
      handlerBuilder = new AnnotationWebSocketHandlerBuilder();
    }

    // configure EndpointParameterResolver
    for (WebSocketConfiguration configurer : configurers) {
      configurer.configureEndpointParameterResolvers(resolvers);
    }

    handlerBuilder.registerDefaultResolvers();
    handlerBuilder.addResolvers(resolvers);
    handlerBuilder.trimToSize(); // @since 4.0 trimToSize
    return handlerBuilder;
  }

  @MissingBean
  MessageBodyEndpointParameterResolver messageBodyEndpointParameterResolver(ObjectMapper jacksonObjectMapper) {
    return new MessageBodyEndpointParameterResolver(jacksonObjectMapper);
  }

}
