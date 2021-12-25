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

import java.lang.reflect.Method;
import java.util.Map;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.Prototypes;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.AnnotationHandlerMethod;
import cn.taketoday.web.handler.HandlerMethodBuilder;
import cn.taketoday.web.handler.MethodParametersBuilder;
import cn.taketoday.web.registry.AbstractUrlHandlerRegistry;
import cn.taketoday.web.socket.annotation.AfterHandshake;
import cn.taketoday.web.socket.annotation.AnnotationHandlerDelegate;
import cn.taketoday.web.socket.annotation.AnnotationWebSocketHandlerBuilder;
import cn.taketoday.web.socket.annotation.EndpointMapping;
import cn.taketoday.web.socket.annotation.OnClose;
import cn.taketoday.web.socket.annotation.OnError;
import cn.taketoday.web.socket.annotation.OnMessage;
import cn.taketoday.web.socket.annotation.OnOpen;
import cn.taketoday.web.socket.annotation.WebSocketHandlerMethod;

/**
 * {@link WebSocketHandler} registry
 *
 * @author TODAY 2021/4/5 12:12
 * @since 3.0
 */
public class WebSocketHandlerRegistry
        extends AbstractUrlHandlerRegistry implements WebApplicationInitializer {
  protected AnnotationWebSocketHandlerBuilder annotationHandlerBuilder;

  public WebSocketHandlerRegistry() { }

  public WebSocketHandlerRegistry(AnnotationWebSocketHandlerBuilder annotationHandlerBuilder) {
    this.annotationHandlerBuilder = annotationHandlerBuilder;
  }

  @Override
  public void onStartup(WebApplicationContext context) throws Throwable {
    if (annotationHandlerBuilder == null) {
      annotationHandlerBuilder = context.getBean(AnnotationWebSocketHandlerBuilder.class);
    }

    Map<String, BeanDefinition> beanDefinitions = context.getBeanDefinitions();
    for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
      BeanDefinition definition = entry.getValue();
      if (isEndpoint(context, definition)) {
        registerEndpoint(definition, context);
      }
    }
  }

  /**
   * Create a handler bean instance
   *
   * @param beanFactory {@link ConfigurableBeanFactory}
   * @return Returns a handler bean of target beanClass
   */
  protected Object createHandler(BeanDefinition def, BeanFactory beanFactory) {
    return def.isSingleton()
           ? beanFactory.getBean(def.getName())
           : Prototypes.newProxyInstance(def.getBeanClass(), def.getName(), beanFactory);
  }

  protected boolean isEndpoint(WebApplicationContext context, BeanDefinition definition) {
    return context.getAnnotationOnBean(
            definition.getName(), EndpointMapping.class) != null;
  }

  protected void registerEndpoint(BeanDefinition definition, WebApplicationContext context) {
    Object handlerBean = createHandler(definition, context);

    Class<?> endpointClass = definition.getBeanClass();

    String[] path = getPath(definition, context);

    Method[] declaredMethods = endpointClass.getDeclaredMethods();

    AnnotationHandlerMethod afterHandshake = null;
    WebSocketHandlerMethod onOpen = null;
    WebSocketHandlerMethod onClose = null;
    WebSocketHandlerMethod onError = null;
    WebSocketHandlerMethod onMessage = null;

    MethodParametersBuilder parameterBuilder = new MethodParametersBuilder();
    HandlerMethodBuilder<AnnotationHandlerMethod> handlerMethodBuilder = new HandlerMethodBuilder<>(context);
    for (Method declaredMethod : declaredMethods) {
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
    AnnotationWebSocketHandlerBuilder handlerBuilder = getAnnotationHandlerBuilder();
    Assert.state(handlerBuilder != null, "No annotationHandlerBuilder in this registry");
    for (String pathPattern : path) {
      AnnotationHandlerDelegate annotationHandler
              = new AnnotationHandlerDelegate(pathPattern, onOpen, onClose, onError, onMessage, afterHandshake);
      WebSocketHandler handler = handlerBuilder.build(definition, context, annotationHandler);
      registerHandler(pathPattern, handler);
    }
  }

  protected String[] getPath(BeanDefinition definition, WebApplicationContext context) {
    EndpointMapping endpointMapping = context.getAnnotationOnBean(
            definition.getName(), EndpointMapping.class);

    return endpointMapping.value();
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
   * apply {@link cn.taketoday.core.PathMatcher}
   */
  @Override
  protected Object lookupInternal(RequestContext context) {
    Object handler = super.lookupInternal(context);
    if (handler != null) {
      context.setAttribute(WebSocketSession.PATH_MATCHER, getPathMatcher());
    }
    return handler;
  }

  public void setAnnotationHandlerBuilder(AnnotationWebSocketHandlerBuilder annotationHandlerBuilder) {
    this.annotationHandlerBuilder = annotationHandlerBuilder;
  }

  public AnnotationWebSocketHandlerBuilder getAnnotationHandlerBuilder() {
    return annotationHandlerBuilder;
  }

}
