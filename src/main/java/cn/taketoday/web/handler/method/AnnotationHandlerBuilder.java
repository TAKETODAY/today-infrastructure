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
import java.util.function.BiFunction;

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
public class AnnotationHandlerBuilder<T extends ActionMappingAnnotationHandler> {

  private ParameterResolvingRegistry resolverRegistry;
  private ReturnValueHandlers returnValueHandlers;
  private MethodParametersBuilder parametersBuilder;

  private BiFunction<BeanSupplier<Object>, Method, T> handlerSupplier;

  public AnnotationHandlerBuilder() { }

  /**
   * this application must have ParameterResolvers bean
   *
   * @param factory Application context or bean factory
   */
  public AnnotationHandlerBuilder(BeanFactory factory) {
    Assert.notNull(factory, "BeanFactory is required");
    ParameterResolvingRegistry resolversRegistry = factory.getBean(ParameterResolvingRegistry.class);
    Assert.state(resolversRegistry != null, "No ParameterResolvers");
    setResolverRegistry(resolversRegistry);
    setReturnValueHandlers(factory.getBean(ReturnValueHandlers.class));
    setParametersBuilder(new ParameterResolversMethodParameterBuilder(resolversRegistry));
  }

  public AnnotationHandlerBuilder(
          ParameterResolvingRegistry resolvers, ReturnValueHandlers returnValueHandlers) {
    this(resolvers, returnValueHandlers, new ParameterResolversMethodParameterBuilder(resolvers));
  }

  public AnnotationHandlerBuilder(
          ParameterResolvingRegistry resolvers,
          ReturnValueHandlers returnValueHandlers, MethodParametersBuilder builder) {
    setParametersBuilder(builder);
    setResolverRegistry(resolvers);
    setReturnValueHandlers(returnValueHandlers);
  }

  public void setHandlerSupplier(BiFunction<BeanSupplier<Object>, Method, T> handlerSupplier) {
    this.handlerSupplier = handlerSupplier;
  }

  /**
   * @see ActionMappingAnnotationHandler#ActionMappingAnnotationHandler(HandlerMethod)
   */
  @SuppressWarnings("unchecked")
  public T build(Object handlerBean, Method method) {
    Assert.state(returnValueHandlers != null, "No ReturnValueHandlers set");
    Assert.state(parametersBuilder != null, "No MethodParametersBuilder set");

    ResolvableMethodParameter[] parameters = parametersBuilder.build(method);

    ActionMappingAnnotationHandler annotationHandler = ActionMappingAnnotationHandler.from(handlerBean, method);
    annotationHandler.getMethod().setParameters(parameters);
    annotationHandler.setReturnValueHandlers(returnValueHandlers);

    return (T) annotationHandler;
  }

  /**
   * @see ActionMappingAnnotationHandler#ActionMappingAnnotationHandler(HandlerMethod)
   */
  public T build(Object handlerBean, Method method, List<HandlerInterceptor> interceptors) {
    T handlerMethod = build(handlerBean, method);
    handlerMethod.setInterceptors(interceptors);
    return handlerMethod;
  }

  public T build(
          BeanSupplier<Object> handlerBean, Method method, @Nullable List<HandlerInterceptor> interceptors) {
    Assert.state(returnValueHandlers != null, "No ReturnValueHandlers set");
    Assert.state(parametersBuilder != null, "No MethodParametersBuilder set");
    ResolvableMethodParameter[] parameters = parametersBuilder.build(method);

    T handler = getHandler(handlerBean, method);

    handler.setReturnValueHandlers(returnValueHandlers);
    handler.setInterceptors(interceptors);
    handler.getMethod().setParameters(parameters);
    return handler;
  }

  @SuppressWarnings("unchecked")
  private T getHandler(BeanSupplier<Object> handlerBean, Method method) {
    if (handlerSupplier != null) {
      return handlerSupplier.apply(handlerBean, method);
    }
    return (T) ActionMappingAnnotationHandler.from(handlerBean, method);
  }

  public void setResolverRegistry(ParameterResolvingRegistry resolverRegistry) {
    this.resolverRegistry = resolverRegistry;
  }

  public void setReturnValueHandlers(ReturnValueHandlers returnValueHandlers) {
    this.returnValueHandlers = returnValueHandlers;
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
