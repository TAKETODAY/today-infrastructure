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

package cn.taketoday.web.handler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.reflect.ConstructorAccessor;
import cn.taketoday.context.reflect.ReflectionException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resolver.ParameterResolvers;
import cn.taketoday.web.view.ResultHandlers;

/**
 * build {@link HandlerMethod}
 *
 * @author TODAY 2021/5/1 13:53
 * @since 3.0
 */
public class HandlerMethodBuilder<T extends HandlerMethod> {

  private ResultHandlers resultHandlers;
  private ParameterResolvers parameterResolvers;
  private MethodParametersBuilder parametersBuilder;

  private ConstructorAccessor constructor;

  public HandlerMethodBuilder() { }

  public HandlerMethodBuilder(ApplicationContext context) {
    Assert.notNull(context, "ApplicationContext must not be null");
    ParameterResolvers parameterResolvers = context.getBean(ParameterResolvers.class);
    Assert.state(parameterResolvers != null, "No ParameterResolvers");
    setParameterResolvers(parameterResolvers);
    setResultHandlers(context.getBean(ResultHandlers.class));
    setParametersBuilder(new ParameterResolversMethodParameterBuilder(parameterResolvers));
  }

  public HandlerMethodBuilder(ParameterResolvers resolvers,
                              ResultHandlers resultHandlers) {
    this(resolvers, resultHandlers, new ParameterResolversMethodParameterBuilder(resolvers));
  }

  public HandlerMethodBuilder(ParameterResolvers resolvers,
                              ResultHandlers resultHandlers,
                              MethodParametersBuilder builder) {
    setParametersBuilder(builder);
    setParameterResolvers(resolvers);
    setResultHandlers(resultHandlers);
  }

  public void setHandlerMethodClass(Class<?> handlerMethodClass) {
    try {
      final Constructor<?> declared = handlerMethodClass.getDeclaredConstructor(Object.class, Method.class);
      this.constructor = ReflectionUtils.newConstructorAccessor(declared);
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException("Target class: '" + handlerMethodClass + "‘ don't exist a suitable constructor");
    }
  }

  public ConstructorAccessor getConstructor() {
    if (constructor == null) {
      setHandlerMethodClass(HandlerMethod.class);
    }
    return constructor;
  }

  /**
   * @see HandlerMethod#HandlerMethod(Object, Method)
   */
  @SuppressWarnings("unchecked")
  public T build(Object handlerBean, Method method) {
    Assert.state(resultHandlers != null, "No ResultHandlers");
    Assert.state(parametersBuilder != null, "No MethodParameterBuilder");

    final T handlerMethod = (T) getConstructor().newInstance(new Object[] { handlerBean, method });
    final MethodParameter[] parameters = parametersBuilder.build(method);
    handlerMethod.setParameters(parameters);
    handlerMethod.setResultHandlers(resultHandlers);

    if (ObjectUtils.isNotEmpty(parameters)) {
      for (MethodParameter parameter : parameters) {
        parameter.setHandlerMethod(handlerMethod);
      }
    }
    return handlerMethod;
  }

  /**
   * @see HandlerMethod#HandlerMethod(Object, Method)
   */
  public T build(Object handlerBean, Method method, List<HandlerInterceptor> interceptors) {
    final T handlerMethod = build(handlerBean, method);
    handlerMethod.setInterceptors(interceptors);
    return handlerMethod;
  }

  public void setParameterResolvers(ParameterResolvers parameterResolvers) {
    this.parameterResolvers = parameterResolvers;
  }

  public void setResultHandlers(ResultHandlers resultHandlers) {
    this.resultHandlers = resultHandlers;
  }

  public void setConstructor(ConstructorAccessor constructor) {
    this.constructor = constructor;
  }

  public void setParametersBuilder(MethodParametersBuilder parameterBuilder) {
    this.parametersBuilder = parameterBuilder;
  }

  public MethodParametersBuilder getParameterBuilder() {
    return parametersBuilder;
  }

  public ParameterResolvers getParameterResolvers() {
    return parameterResolvers;
  }

  public ResultHandlers getResultHandlers() {
    return resultHandlers;
  }
}
