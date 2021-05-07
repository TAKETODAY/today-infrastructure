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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.factory.Prototypes;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.registry.AbstractUrlHandlerRegistry;
import cn.taketoday.web.socket.annotation.AfterHandshake;
import cn.taketoday.web.socket.annotation.AnnotationWebSocketDispatcher;
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
public class WebSocketHandlerRegistry extends AbstractUrlHandlerRegistry implements WebApplicationInitializer {

  protected final List<Class<? extends Annotation>> eventClass = new LinkedList<>();

  public WebSocketHandlerRegistry() {
    eventClass.add(OnError.class);
    eventClass.add(OnOpen.class);
    eventClass.add(OnClose.class);
    eventClass.add(OnMessage.class);
    eventClass.add(AfterHandshake.class);
  }

  @SafeVarargs
  public final void addEventClass(Class<? extends Annotation>... classes) {
    Collections.addAll(eventClass, classes);
  }

  @Override
  public void onStartup(WebApplicationContext context) throws Throwable {
    final Map<String, BeanDefinition> beanDefinitions = context.getBeanDefinitions();

    final List<Class<? extends Annotation>> eventClass = this.eventClass;
    for (final Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
      final BeanDefinition definition = entry.getValue();
      if (isEndpoint(definition)) {
        final Class<?> endpointClass = definition.getBeanClass();
        final EndpointMapping endpointMapping = definition.getAnnotation(EndpointMapping.class);
        final String[] path = endpointMapping.value();

        AnnotationWebSocketDispatcher dispatcher = new AnnotationWebSocketDispatcher();
        final Method[] declaredMethods = endpointClass.getDeclaredMethods();

        final Object handlerBean = createHandler(definition, context);
        for (final Method declaredMethod : declaredMethods) {
          for (final Class<? extends Annotation> aClass : eventClass) {
            if (declaredMethod.isAnnotationPresent(aClass)) {
              WebSocketHandlerMethod handlerMethod = new WebSocketHandlerMethod(handlerBean, declaredMethod);

            }
          }
        }

        registerHandler(dispatcher, path);
      }
    }
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

  protected void registerEndpoint(BeanDefinition definition) {

    final Class<?> endpointClass = definition.getBeanClass();
    final EndpointMapping endpointMapping = definition.getAnnotation(EndpointMapping.class);
    final String[] path = endpointMapping.value();

    WebSocketHandler handler = new AnnotationWebSocketDispatcher();
    final Method[] declaredMethods = endpointClass.getDeclaredMethods();
    for (final Method declaredMethod : declaredMethods) {

//      if (isOnOpenHandler(declaredMethod, definition)) {
//        onOpen(declaredMethod, definition);
//      }
//      else if (isOnCloseHandler(declaredMethod, definition)) {
//        onClose(declaredMethod, definition);
//      }
//      else if (isAfterHandshakeHandler(declaredMethod, definition)) {
//        afterHandshake(declaredMethod, definition);
//      }
//      else if (isOnErrorHandler(declaredMethod, definition)) {
//        onError(declaredMethod, definition);
//      }
//      else if (isOnMessageHandler(declaredMethod, definition)) {
//        onMessage(declaredMethod, definition);
//      }
    }

    registerHandler(handler, path);
  }

  protected boolean isOnMessageHandler(Method declaredMethod, BeanDefinition definition) {
    return declaredMethod.isAnnotationPresent(OnError.class);
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

  protected WebSocketHandler onOpen(Method declaredMethod, BeanDefinition definition) {

    return null;
  }

  protected WebSocketHandler onMessage(Method declaredMethod, BeanDefinition definition) {

    return null;
  }

  protected WebSocketHandler onError(Method declaredMethod, BeanDefinition definition) {
    return null;
  }

  protected WebSocketHandler onClose(Method declaredMethod, BeanDefinition definition) {
    return null;
  }

  protected WebSocketHandler afterHandshake(Method declaredMethod, BeanDefinition definition) {
    return null;
  }
  // ServerEndpointDelegate

}
