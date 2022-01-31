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
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.view.ReturnValueHandlers;

/**
 * build {@link ActionMappingAnnotationHandler}
 *
 * @author TODAY 2021/5/1 13:53
 * @since 3.0
 */
public class AnnotationHandlerFactory<T extends ActionMappingAnnotationHandler> {

  private ReturnValueHandlers returnValueHandlers;
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
    setReturnValueHandlers(factory.getBean(ReturnValueHandlers.class));
    setParameterFactory(new ParameterResolvingRegistryResolvableParameterFactory(registry));
  }

  /**
   * @see ActionMappingAnnotationHandler#ActionMappingAnnotationHandler(HandlerMethod, ResolvableMethodParameter[])
   */
  @SuppressWarnings("unchecked")
  public T create(Object handlerBean, Method method) {
    Assert.state(returnValueHandlers != null, "No ReturnValueHandlers set");
    Assert.state(parameterFactory != null, "No ResolvableParameterFactory set");

    ActionMappingAnnotationHandler handler = ActionMappingAnnotationHandler.from(
            handlerBean, method, parameterFactory);
    handler.setReturnValueHandlers(returnValueHandlers);
    return (T) handler;
  }

  /**
   * @see ActionMappingAnnotationHandler#ActionMappingAnnotationHandler(HandlerMethod, ResolvableMethodParameter[])
   */
  public T create(Object handlerBean, Method method, List<HandlerInterceptor> interceptors) {
    T handlerMethod = create(handlerBean, method);
    handlerMethod.setInterceptors(interceptors);
    return handlerMethod;
  }

  @SuppressWarnings("unchecked")
  public T create(
          BeanSupplier<Object> handlerBean, Method method, @Nullable List<HandlerInterceptor> interceptors) {
    Assert.state(returnValueHandlers != null, "No ReturnValueHandlers set");
    Assert.state(parameterFactory != null, "No ResolvableParameterFactory set");

    T handler = (T) ActionMappingAnnotationHandler.from(handlerBean, method, parameterFactory);
    handler.setInterceptors(interceptors);
    handler.setReturnValueHandlers(returnValueHandlers);
    return handler;
  }

  public void setReturnValueHandlers(ReturnValueHandlers returnValueHandlers) {
    this.returnValueHandlers = returnValueHandlers;
  }

  public void setParameterFactory(ResolvableParameterFactory parameterFactory) {
    this.parameterFactory = parameterFactory;
  }

}
