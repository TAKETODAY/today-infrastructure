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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.factory.Prototypes;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.HandlerMethodBuilder;
import cn.taketoday.web.handler.MethodParametersBuilder;
import cn.taketoday.web.registry.AbstractUrlHandlerRegistry;
import cn.taketoday.web.socket.annotation.AfterHandshake;
import cn.taketoday.web.socket.annotation.AnnotationWebSocketDispatcher;
import cn.taketoday.web.socket.annotation.AnnotationWebSocketHandler;
import cn.taketoday.web.socket.annotation.EndpointMapping;
import cn.taketoday.web.socket.annotation.EndpointParameterResolver;
import cn.taketoday.web.socket.annotation.JettySessionEndpointParameterResolver;
import cn.taketoday.web.socket.annotation.OnClose;
import cn.taketoday.web.socket.annotation.OnError;
import cn.taketoday.web.socket.annotation.OnMessage;
import cn.taketoday.web.socket.annotation.OnOpen;
import cn.taketoday.web.socket.annotation.PathVariableEndpointParameterResolver;
import cn.taketoday.web.socket.annotation.WebSocketHandlerMethod;
import cn.taketoday.web.socket.annotation.WebSocketSessionEndpointParameterResolver;

/**
 * {@link WebSocketHandler} registry
 *
 * @author TODAY 2021/4/5 12:12
 * @since 3.0
 */
public class WebSocketHandlerRegistry
        extends AbstractUrlHandlerRegistry implements WebApplicationInitializer {
  protected static boolean isJettyPresent = ClassUtils.isPresent("org.eclipse.jetty.websocket.api.Session");

  protected final List<EndpointParameterResolver> resolvers = new ArrayList<>();

  @Override
  public void onStartup(WebApplicationContext context) throws Throwable {
    final Map<String, BeanDefinition> beanDefinitions = context.getBeanDefinitions();

    for (final Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
      final BeanDefinition definition = entry.getValue();
      if (isEndpoint(definition)) {
        registerEndpoint(definition, context);
      }
    }

    if (isJettyPresent) {
      resolvers.add(new JettySessionEndpointParameterResolver());
    }
    resolvers.add(new PathVariableEndpointParameterResolver());
    resolvers.add(new WebSocketSessionEndpointParameterResolver());
  }

  /**
   * Create a handler bean instance
   *
   * @param beanFactory
   *         {@link ConfigurableBeanFactory}
   *
   * @return Returns a handler bean of target beanClass
   */
  protected Object createHandler(final BeanDefinition def, final ConfigurableBeanFactory beanFactory) {
    return def.isSingleton()
           ? beanFactory.getBean(def)
           : Prototypes.newProxyInstance(def.getBeanClass(), def, beanFactory);
  }

  protected boolean isEndpoint(BeanDefinition definition) {
    return definition.isAnnotationPresent(EndpointMapping.class);
  }

  protected void registerEndpoint(BeanDefinition definition, WebApplicationContext context) {
    final Object handlerBean = createHandler(definition, context);

    final Class<?> endpointClass = definition.getBeanClass();
    final EndpointMapping endpointMapping = definition.getAnnotation(EndpointMapping.class);
    final String[] path = endpointMapping.value();

    final Method[] declaredMethods = endpointClass.getDeclaredMethods();

    HandlerMethod afterHandshake = null;
    WebSocketHandlerMethod onOpen = null;
    WebSocketHandlerMethod onClose = null;
    WebSocketHandlerMethod onError = null;
    WebSocketHandlerMethod onMessage = null;

    final MethodParametersBuilder parameterBuilder = new MethodParametersBuilder();
    HandlerMethodBuilder<HandlerMethod> handlerMethodBuilder = new HandlerMethodBuilder<>(context);
    for (final Method declaredMethod : declaredMethods) {
      if (isOnOpenHandler(declaredMethod, definition)) {
        onOpen = new WebSocketHandlerMethod(handlerBean, declaredMethod, parameterBuilder);
      }
      else if (isOnCloseHandler(declaredMethod, definition)) {
        onClose = new WebSocketHandlerMethod(handlerBean, declaredMethod, parameterBuilder);
      }
      else if (isAfterHandshakeHandler(declaredMethod, definition)) {
        afterHandshake = handlerMethodBuilder.build(handlerBean, declaredMethod);
      }
      else if (isOnErrorHandler(declaredMethod, definition)) {
        onError = new WebSocketHandlerMethod(handlerBean, declaredMethod, parameterBuilder);
      }
      else if (isOnMessageHandler(declaredMethod, definition)) {
        onMessage = new WebSocketHandlerMethod(handlerBean, declaredMethod, parameterBuilder);
      }
    }

    for (final String pathPattern : path) {
      final AnnotationWebSocketHandler annotationHandler
              = new AnnotationWebSocketHandler(pathPattern, onOpen, onClose, onError, onMessage, afterHandshake);
      WebSocketHandler handler = createWebSocketHandler(definition, context, annotationHandler);
      registerHandler(pathPattern, handler);
    }
  }

  protected WebSocketHandler createWebSocketHandler(BeanDefinition definition,
                                                    WebApplicationContext context,
                                                    AnnotationWebSocketHandler annotationHandler) {
    return new AnnotationWebSocketDispatcher(annotationHandler, resolvers);
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

  //

  /**
   * apply {@link cn.taketoday.context.PathMatcher}
   */
  @Override
  protected Object lookupInternal(RequestContext context) {
    final Object handler = super.lookupInternal(context);
    if (handler != null) {
      context.setAttribute(WebSocketSession.PATH_MATCHER, getPathMatcher());
    }
    return handler;
  }
}
