/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.config.WebMvcConfigurationSupport;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.AnnotationHandlerFactory;
import cn.taketoday.web.handler.method.ResolvableParameterFactory;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.annotation.AfterHandshake;
import cn.taketoday.web.socket.annotation.AnnotationWebSocketHandlerBuilder;
import cn.taketoday.web.socket.annotation.EndpointMapping;
import cn.taketoday.web.socket.annotation.EndpointParameterResolver;
import cn.taketoday.web.socket.annotation.MessageBodyEndpointParameterResolver;
import cn.taketoday.web.socket.annotation.OnClose;
import cn.taketoday.web.socket.annotation.OnError;
import cn.taketoday.web.socket.annotation.OnMessage;
import cn.taketoday.web.socket.annotation.OnOpen;
import cn.taketoday.web.socket.annotation.WebSocketHandlerDelegate;
import cn.taketoday.web.socket.annotation.WebSocketHandlerMethod;
import cn.taketoday.web.socket.annotation.WebSocketSessionParameterResolver;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.support.DefaultHandshakeHandler;
import cn.taketoday.web.socket.server.support.WebSocketHandlerMapping;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Configuration support for WebSocket request handling.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebSocketConfigurationSupport {

  @Component
  public WebSocketHandlerMapping webSocketHandlerMapping(ApplicationContext context,
          @Nullable WebMvcConfigurationSupport support,
          AnnotationWebSocketHandlerBuilder handlerBuilder,
          @Nullable RequestUpgradeStrategy requestUpgradeStrategy) {

    DefaultWebSocketHandlerRegistry registry = new DefaultWebSocketHandlerRegistry();
    if (requestUpgradeStrategy != null) {
      registry.setHandshakeHandler(new DefaultHandshakeHandler(requestUpgradeStrategy));
    }

    registerWebSocketHandlers(registry);
    onStartup(context, registry, handlerBuilder);
    WebSocketHandlerMapping handlerMapping = registry.getHandlerMapping();
    if (support != null) {
      support.initHandlerMapping(handlerMapping);
    }
    return handlerMapping;
  }

  protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

  }

  @MissingBean
  WebSocketSessionParameterResolver webSocketSessionParameterResolver() {
    return new WebSocketSessionParameterResolver();
  }

  @MissingBean(value = AnnotationWebSocketHandlerBuilder.class)
  AnnotationWebSocketHandlerBuilder annotationWebSocketHandlerBuilder(
          List<EndpointParameterResolver> resolvers) {

    var handlerBuilder = new AnnotationWebSocketHandlerBuilder();

    registerEndpointParameterStrategies(resolvers);

    handlerBuilder.registerDefaultResolvers();
    handlerBuilder.addResolvers(resolvers);
    handlerBuilder.trimToSize(); // @since 4.0 trimToSize
    return handlerBuilder;
  }

  protected void registerEndpointParameterStrategies(List<EndpointParameterResolver> resolvers) { }

  @MissingBean
  MessageBodyEndpointParameterResolver messageBodyEndpointParameterResolver(
          @Nullable WebMvcConfigurationSupport support) {
    if (support == null) {
      support = new WebMvcConfigurationSupport();
    }

    List<HttpMessageConverter<?>> messageConverters = support.getMessageConverters();
    return new MessageBodyEndpointParameterResolver(messageConverters);
  }

  protected void onStartup(ApplicationContext context,
          WebSocketHandlerRegistry handlerRegistry, AnnotationWebSocketHandlerBuilder handlerBuilder) {
    AnnotationHandlerFactory factory = context.getBean(AnnotationHandlerFactory.class);

    ConfigurableBeanFactory beanFactory = context.unwrapFactory(ConfigurableBeanFactory.class);
    String[] definitionNames = context.getBeanDefinitionNames();
    for (String beanName : definitionNames) {
      BeanDefinition merged = beanFactory.getMergedBeanDefinition(beanName);
      if (!merged.isAbstract() && merged instanceof RootBeanDefinition root) {
        if (isEndpoint(context, root, beanName)) {
          registerEndpoint(beanName, root, context, factory, handlerBuilder, handlerRegistry);
        }
      }
    }
  }

  /**
   * Create a handler bean instance
   *
   * @param beanName bean name
   * @param beanFactory {@link ConfigurableBeanFactory}
   * @return Returns a handler bean of target beanClass
   */
  protected BeanSupplier<Object> createHandler(String beanName, BeanFactory beanFactory) {
    return BeanSupplier.from(beanFactory, beanName);
  }

  protected boolean isEndpoint(ApplicationContext context, BeanDefinition definition, String beanName) {
    return context.findAnnotationOnBean(
            beanName, EndpointMapping.class).isPresent();
  }

  protected void registerEndpoint(
          String beanName, RootBeanDefinition definition, ApplicationContext context,
          AnnotationHandlerFactory factory, AnnotationWebSocketHandlerBuilder handlerBuilder,
          WebSocketHandlerRegistry registry) {
    BeanSupplier<Object> handlerBean = createHandler(beanName, context);

    Class<?> endpointClass = definition.getBeanClass();
    String[] pathPatterns = getPath(beanName, definition, context);
    Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(endpointClass);

    ActionMappingAnnotationHandler afterHandshake = null;
    WebSocketHandlerMethod onOpen = null;
    WebSocketHandlerMethod onClose = null;
    WebSocketHandlerMethod onError = null;
    WebSocketHandlerMethod onMessage = null;

    ResolvableParameterFactory parameterFactory = new ResolvableParameterFactory();
    for (Method declaredMethod : declaredMethods) {
      if (isOnOpenHandler(declaredMethod, definition)) {
        onOpen = new WebSocketHandlerMethod(handlerBean, declaredMethod, parameterFactory);
      }
      else if (isOnCloseHandler(declaredMethod, definition)) {
        onClose = new WebSocketHandlerMethod(handlerBean, declaredMethod, parameterFactory);
      }
      else if (isAfterHandshakeHandler(declaredMethod, definition)) {
        afterHandshake = factory.create(handlerBean, declaredMethod, endpointClass, null);
      }
      else if (isOnErrorHandler(declaredMethod, definition)) {
        onError = new WebSocketHandlerMethod(handlerBean, declaredMethod, parameterFactory);
      }
      else if (isOnMessageHandler(declaredMethod, definition)) {
        onMessage = new WebSocketHandlerMethod(handlerBean, declaredMethod, parameterFactory);
      }
    }

    // TODO make PathPatternParser configurable
    PathPatternParser patternParser = PathPatternParser.defaultInstance;
    for (String pattern : pathPatterns) {
      PathPattern pathPattern = patternParser.parse(pattern);

      boolean containsPathVariable = !pathPattern.getVariableNames().isEmpty();
      var annotationHandler = new WebSocketHandlerDelegate(
              pathPattern, containsPathVariable, onOpen, onClose, onError, onMessage, afterHandshake);
      WebSocketHandler handler = handlerBuilder.build(beanName, definition, context, annotationHandler);
      registry.addHandler(handler, pattern);
    }
  }

  protected String[] getPath(String beanName, BeanDefinition definition, ApplicationContext context) {
    return context.findAnnotationOnBean(beanName, EndpointMapping.class).getStringValueArray();
  }

  protected boolean isOnMessageHandler(Method declaredMethod, BeanDefinition definition) {
    return declaredMethod.isAnnotationPresent(OnMessage.class);
  }

  protected boolean isOnErrorHandler(Method declaredMethod, BeanDefinition definition) {
    return declaredMethod.isAnnotationPresent(OnError.class);
  }

  protected boolean isAfterHandshakeHandler(Method declaredMethod, BeanDefinition definition) {
    return declaredMethod.isAnnotationPresent(AfterHandshake.class);
  }

  protected boolean isOnCloseHandler(Method declaredMethod, BeanDefinition definition) {
    return declaredMethod.isAnnotationPresent(OnClose.class);
  }

  protected boolean isOnOpenHandler(Method declaredMethod, BeanDefinition definition) {
    return declaredMethod.isAnnotationPresent(OnOpen.class);
  }

}
