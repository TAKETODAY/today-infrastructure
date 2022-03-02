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

package cn.taketoday.web.handler.method;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * build {@link ActionMappingAnnotationHandler}
 *
 * @author TODAY 2021/5/1 13:53
 * @since 3.0
 */
public class AnnotationHandlerFactory<T extends ActionMappingAnnotationHandler> {

  private ReturnValueHandlerManager returnValueHandlerManager;
  private ResolvableParameterFactory parameterFactory;

  public AnnotationHandlerFactory() { }

  /**
   * this application must have ParameterResolvers bean
   *
   * @param factory Application context or bean factory
   */
  public AnnotationHandlerFactory(BeanFactory factory) {
    Assert.notNull(factory, "BeanFactory is required");
    ParameterResolvingRegistry registry = factory.getBean(ParameterResolvingRegistry.class);
    Assert.state(registry != null, "No ParameterResolvingRegistry");
    setReturnValueHandlers(factory.getBean(ReturnValueHandlerManager.class));
    setParameterFactory(new ParameterResolvingRegistryResolvableParameterFactory(registry));
  }

  /**
   * @see ActionMappingAnnotationHandler#ActionMappingAnnotationHandler(HandlerMethod, ResolvableMethodParameter[], Class)
   */
  @SuppressWarnings("unchecked")
  public T create(Object handlerBean, Method method, Class<?> beanType) {
    Assert.state(returnValueHandlerManager != null, "No ReturnValueHandlers set");
    Assert.state(parameterFactory != null, "No ResolvableParameterFactory set");

    ActionMappingAnnotationHandler handler = ActionMappingAnnotationHandler.from(
            handlerBean, method, parameterFactory, beanType);
    handler.setReturnValueHandlers(returnValueHandlerManager);
    return (T) handler;
  }

  /**
   * @see ActionMappingAnnotationHandler#ActionMappingAnnotationHandler(HandlerMethod, ResolvableMethodParameter[], Class)
   */
  public T create(Object handlerBean, Method method, Class<?> beanType, List<HandlerInterceptor> interceptors) {
    T handlerMethod = create(handlerBean, method, beanType);
    handlerMethod.setInterceptors(interceptors);
    return handlerMethod;
  }

  @SuppressWarnings("unchecked")
  public T create(
          BeanSupplier<Object> handlerBean, Method method, Class<?> beanType, @Nullable List<HandlerInterceptor> interceptors) {
    Assert.state(returnValueHandlerManager != null, "No ReturnValueHandlers set");
    Assert.state(parameterFactory != null, "No ResolvableParameterFactory set");
    T handler = (T) ActionMappingAnnotationHandler.from(handlerBean, method, parameterFactory, beanType);
    handler.setInterceptors(interceptors);
    handler.setReturnValueHandlers(returnValueHandlerManager);
    return handler;
  }

  public void setReturnValueHandlers(ReturnValueHandlerManager manager) {
    this.returnValueHandlerManager = manager;
  }

  public void setParameterFactory(ResolvableParameterFactory parameterFactory) {
    this.parameterFactory = parameterFactory;
  }

}
