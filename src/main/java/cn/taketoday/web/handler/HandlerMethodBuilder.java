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

package cn.taketoday.web.handler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.view.ReturnValueHandlers;

/**
 * build {@link HandlerMethod}
 *
 * @author TODAY 2021/5/1 13:53
 * @since 3.0
 */
public class HandlerMethodBuilder<T extends AnnotationHandlerMethod> {

  private ParameterResolvingRegistry resolverRegistry;
  private ReturnValueHandlers returnValueHandlers;
  private MethodParametersBuilder parametersBuilder;

  private BeanInstantiator constructor;

  public HandlerMethodBuilder() { }

  /**
   * this application must have ParameterResolvers bean
   *
   * @param context Application context or bean factory
   */
  public HandlerMethodBuilder(ApplicationContext context) {
    Assert.notNull(context, "ApplicationContext must not be null");
    ParameterResolvingRegistry resolversRegistry = context.getBean(ParameterResolvingRegistry.class);
    Assert.state(resolversRegistry != null, "No ParameterResolvers");
    setResolverRegistry(resolversRegistry);
    setReturnValueHandlers(context.getBean(ReturnValueHandlers.class));
    setParametersBuilder(new ParameterResolversMethodParameterBuilder(resolversRegistry));
  }

  public HandlerMethodBuilder(
          ParameterResolvingRegistry resolvers, ReturnValueHandlers returnValueHandlers) {
    this(resolvers, returnValueHandlers, new ParameterResolversMethodParameterBuilder(resolvers));
  }

  public HandlerMethodBuilder(
          ParameterResolvingRegistry resolvers,
          ReturnValueHandlers returnValueHandlers, MethodParametersBuilder builder) {
    setParametersBuilder(builder);
    setResolverRegistry(resolvers);
    setReturnValueHandlers(returnValueHandlers);
  }

  public void setHandlerMethodClass(Class<?> handlerMethodClass) {
    try {
      Constructor<?> declared = handlerMethodClass.getDeclaredConstructor(Object.class, Method.class);
      this.constructor = BeanInstantiator.fromConstructor(declared);
    }
    catch (NoSuchMethodException e) {
      throw new ConfigurationException(
              "Target class: '" + handlerMethodClass + "' don't exist a suitable constructor", e);
    }
  }

  public BeanInstantiator getConstructor() {
    if (constructor == null) {
      setHandlerMethodClass(HandlerMethod.class);
    }
    return constructor;
  }

  /**
   * @see AnnotationHandlerMethod#AnnotationHandlerMethod(HandlerMethod)
   */
  @SuppressWarnings("unchecked")
  public T build(Object handlerBean, Method method) {
    Assert.state(returnValueHandlers != null, "No ResultHandlers set");
    Assert.state(parametersBuilder != null, "No MethodParametersBuilder set");

    T handler = (T) getConstructor().instantiate(new Object[] { method });
    MethodParameter[] parameters = parametersBuilder.build(method);
    HandlerMethod handlerMethod = handler.getMethod();

    handlerMethod.setParameters(parameters);
    handler.setResultHandlers(returnValueHandlers);
    if (ObjectUtils.isNotEmpty(parameters)) {
      for (MethodParameter parameter : parameters) {
        parameter.setHandlerMethod(handlerMethod);
      }
    }
    return handler;
  }

  /**
   * @see AnnotationHandlerMethod#AnnotationHandlerMethod(HandlerMethod)
   */
  public T build(Object handlerBean, Method method, List<HandlerInterceptor> interceptors) {
    T handlerMethod = build(handlerBean, method);
    handlerMethod.setInterceptors(interceptors);
    return handlerMethod;
  }

  @SuppressWarnings("unchecked")
  public T build(BeanSupplier<Object> handlerBean, Method method, List<HandlerInterceptor> interceptors) {
    Assert.state(returnValueHandlers != null, "No ResultHandlers set");
    Assert.state(parametersBuilder != null, "No MethodParametersBuilder set");
    T handler = (T) getConstructor().instantiate(new Object[] { method });
    MethodParameter[] parameters = parametersBuilder.build(method);

    HandlerMethod handlerMethod = HandlerMethod.from(method);
    handlerMethod.setParameters(parameters);

    handler.setInterceptors(interceptors);
    handler.setResultHandlers(returnValueHandlers);

    if (ObjectUtils.isNotEmpty(parameters)) {
      for (MethodParameter parameter : parameters) {
        parameter.setHandlerMethod(handlerMethod);
      }
    }
    return handler;
  }

  public void setResolverRegistry(ParameterResolvingRegistry resolverRegistry) {
    this.resolverRegistry = resolverRegistry;
  }

  public void setReturnValueHandlers(ReturnValueHandlers returnValueHandlers) {
    this.returnValueHandlers = returnValueHandlers;
  }

  public void setConstructor(BeanInstantiator constructor) {
    this.constructor = constructor;
  }

  public void setParametersBuilder(MethodParametersBuilder parameterBuilder) {
    this.parametersBuilder = parameterBuilder;
  }

  public MethodParametersBuilder getParameterBuilder() {
    return parametersBuilder;
  }

  public ParameterResolvingRegistry getResolverRegistry() {
    return resolverRegistry;
  }

  public ReturnValueHandlers getReturnValueHandlers() {
    return returnValueHandlers;
  }
}
