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

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.AnnotationHandlerFactory;
import cn.taketoday.web.handler.method.ResolvableParameterFactory;
import cn.taketoday.web.registry.AbstractUrlHandlerRegistry;
import cn.taketoday.web.registry.annotation.HandlerMethodRegistry;
import cn.taketoday.web.socket.annotation.AfterHandshake;
import cn.taketoday.web.socket.annotation.AnnotationWebSocketHandlerBuilder;
import cn.taketoday.web.socket.annotation.EndpointMapping;
import cn.taketoday.web.socket.annotation.OnClose;
import cn.taketoday.web.socket.annotation.OnError;
import cn.taketoday.web.socket.annotation.OnMessage;
import cn.taketoday.web.socket.annotation.OnOpen;
import cn.taketoday.web.socket.annotation.WebSocketHandlerDelegate;
import cn.taketoday.web.socket.annotation.WebSocketHandlerMethod;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

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
    AnnotationHandlerFactory factory = context.getBean(AnnotationHandlerFactory.class);

    ConfigurableBeanFactory beanFactory = context.getBeanFactory().unwrap(ConfigurableBeanFactory.class);
    String[] definitionNames = context.getBeanDefinitionNames();
    for (String beanName : definitionNames) {
      BeanDefinition merged = beanFactory.getMergedBeanDefinition(beanName);
      if (!merged.isAbstract() && merged instanceof RootBeanDefinition root) {
        if (isEndpoint(context, root, beanName)) {
          registerEndpoint(beanName, root, context, factory);
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

  protected boolean isEndpoint(WebApplicationContext context, BeanDefinition definition, String beanName) {
    return context.findAnnotationOnBean(
            beanName, EndpointMapping.class).isPresent();
  }

  protected void registerEndpoint(
          String beanName, RootBeanDefinition definition, WebApplicationContext context,
          AnnotationHandlerFactory factory) {
    BeanSupplier<Object> handlerBean = createHandler(beanName, context);

    Class<?> endpointClass = definition.getBeanClass();

    String[] path = getPath(beanName, definition, context);

    Method[] declaredMethods = endpointClass.getDeclaredMethods();

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
    AnnotationWebSocketHandlerBuilder handlerBuilder = getAnnotationHandlerBuilder();
    Assert.state(handlerBuilder != null, "No annotationHandlerBuilder in this registry");
    PathPatternParser patternParser = getPatternParser();
    for (String pattern : path) {
      PathPattern pathPattern = patternParser.parse(pattern);
      boolean containsPathVariable = HandlerMethodRegistry.containsPathVariable(pattern);
      WebSocketHandlerDelegate annotationHandler = new WebSocketHandlerDelegate(
              pathPattern, containsPathVariable, onOpen, onClose, onError, onMessage, afterHandshake);
      WebSocketHandler handler = handlerBuilder.build(beanName, definition, context, annotationHandler);
      registerHandler(pattern, handler);
    }
  }

  protected String[] getPath(String beanName, BeanDefinition definition, WebApplicationContext context) {
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

  //

  public void setAnnotationHandlerBuilder(AnnotationWebSocketHandlerBuilder annotationHandlerBuilder) {
    this.annotationHandlerBuilder = annotationHandlerBuilder;
  }

  public AnnotationWebSocketHandlerBuilder getAnnotationHandlerBuilder() {
    return annotationHandlerBuilder;
  }

}
