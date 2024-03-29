/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.StandardEndpoint;
import cn.taketoday.web.socket.WebSocketHandler;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import static cn.taketoday.util.ClassUtils.isPresent;

/**
 * @author TODAY 2021/5/12 23:30
 * @since 3.0.1
 */
public class AnnotationWebSocketHandlerBuilder implements ArraySizeTrimmer {
  protected static boolean isJettyPresent = isPresent("org.eclipse.jetty.websocket.api.Session", AnnotationWebSocketHandlerBuilder.class);

  protected final ArrayList<EndpointParameterResolver> resolvers = new ArrayList<>(16);

  protected boolean supportPartialMessage;

  private ConversionService conversionService = ApplicationConversionService.getSharedInstance();

  /**
   * register default resolvers
   */
  public void registerDefaultResolvers() {
    if (isJettyPresent) {
      resolvers.add(new JettySessionEndpointParameterResolver());
    }
    resolvers.add(new IsLastEndpointParameterResolver());
    resolvers.add(new MessageEndpointParameterResolver(String.class, conversionService));
    resolvers.add(new MessageEndpointParameterResolver(byte[].class, source -> ((String) source).getBytes(StandardCharsets.UTF_8)));
    resolvers.add(new MessageEndpointParameterResolver(ByteBuffer.class, conversionService));
    resolvers.add(new PathVariableEndpointParameterResolver(conversionService));
    resolvers.add(new WebSocketSessionEndpointParameterResolver());

    if (ServletDetector.isWebSocketPresent) {
      resolvers.add(new PathParamEndpointParameterResolver(conversionService));
      resolvers.add(new EndpointConfigEndpointParameterResolver());
      resolvers.add(new StandardSessionEndpointParameterResolver());
    }
  }

  public void addResolvers(EndpointParameterResolver... resolvers) {
    Assert.notNull(resolvers, "EndpointParameterResolvers is required");
    Collections.addAll(this.resolvers, resolvers);
  }

  public void addResolvers(List<EndpointParameterResolver> resolvers) {
    Assert.notNull(resolvers, "EndpointParameterResolvers is required");
    this.resolvers.addAll(resolvers);
  }

  public void setResolvers(@Nullable EndpointParameterResolver... resolvers) {
    this.resolvers.clear();
    CollectionUtils.addAll(this.resolvers, resolvers);
  }

  public void setResolvers(@Nullable List<EndpointParameterResolver> resolvers) {
    this.resolvers.clear();
    CollectionUtils.addAll(this.resolvers, resolvers);
  }

  /**
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    CollectionUtils.trimToSize(resolvers);
  }

  public WebSocketHandler build(String beanName, BeanDefinition definition,
          ApplicationContext context, WebSocketHandlerDelegate annotationHandler) {
    if (ServletDetector.isWebSocketPresent) {
      var socketDispatcher = new StandardAnnotationWebSocketDispatcher(
              annotationHandler, resolvers, supportPartialMessage);
      var serverEndpoint = context.findAnnotationOnBean(beanName, ServerEndpoint.class);
      if (serverEndpoint.isPresent()) {
        ServerEndpointConfig.Configurator configuratorObject = null;
        var configurator = serverEndpoint.<ServerEndpointConfig.Configurator>getClass("configurator");
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
    return new AnnotationWebSocketDispatcher(annotationHandler, resolvers, supportPartialMessage);
  }

  /**
   * set support Partial Message
   *
   * @param supportPartialMessage supportPartialMessage?
   * @see Message#isLast()
   * @see WebSocketHandler#supportsPartialMessage()
   */
  public void setSupportPartialMessage(boolean supportPartialMessage) {
    this.supportPartialMessage = supportPartialMessage;
  }

  public boolean isSupportPartialMessage() {
    return supportPartialMessage;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  /**
   * @param conversionService conversionService
   * @see DefaultConversionService#getSharedInstance()
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    if (conversionService == null) {
      conversionService = DefaultConversionService.getSharedInstance();
    }
    this.conversionService = conversionService;
  }

}
