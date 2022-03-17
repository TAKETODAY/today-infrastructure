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
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * build {@link ActionMappingAnnotationHandler}
 *
 * @author TODAY 2021/5/1 13:53
 * @since 3.0
 */
public class AnnotationHandlerFactory {

  private final BeanFactory beanFactory;

  @Nullable
  private ReturnValueHandlerManager returnValueHandlerManager;

  private ResolvableParameterFactory parameterFactory;

  /**
   * this application must have ParameterResolvers bean
   *
   * @param beanFactory Application context or bean factory
   */
  public AnnotationHandlerFactory(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanFactory = beanFactory;
  }

  public void initDefaults() {
    ParameterResolvingRegistry registry = beanFactory.getBean(ParameterResolvingRegistry.class);
    Assert.state(registry != null, "No ParameterResolvingRegistry");
    setReturnValueHandlerManager(beanFactory.getBean(ReturnValueHandlerManager.class));
    setParameterFactory(new ParameterResolvingRegistryResolvableParameterFactory(registry));
  }

  /**
   * @see ActionMappingAnnotationHandler#ActionMappingAnnotationHandler(HandlerMethod, ResolvableMethodParameter[], Class)
   */
  public ActionMappingAnnotationHandler create(Object handlerBean, Method method, Class<?> beanType) {
    Assert.state(returnValueHandlerManager != null, "No ReturnValueHandlers set");
    Assert.state(parameterFactory != null, "No ResolvableParameterFactory set");

    ActionMappingAnnotationHandler handler = ActionMappingAnnotationHandler.from(
            handlerBean, method, parameterFactory, beanType);
    handler.setReturnValueHandlers(returnValueHandlerManager);
    return handler;
  }

  /**
   * @see ActionMappingAnnotationHandler#ActionMappingAnnotationHandler(HandlerMethod, ResolvableMethodParameter[], Class)
   */
  public ActionMappingAnnotationHandler create(Object handlerBean, Method method, Class<?> beanType, List<HandlerInterceptor> interceptors) {
    var handlerMethod = create(handlerBean, method, beanType);
    handlerMethod.setInterceptors(interceptors);
    return handlerMethod;
  }

  public ActionMappingAnnotationHandler create(
          Supplier<Object> handlerBean, Method method, Class<?> beanType, @Nullable List<HandlerInterceptor> interceptors) {
    Assert.state(returnValueHandlerManager != null, "No ReturnValueHandlers set");
    Assert.state(parameterFactory != null, "No ResolvableParameterFactory set");
    var handler = ActionMappingAnnotationHandler.from(handlerBean, method, parameterFactory, beanType);
    handler.setInterceptors(interceptors);
    handler.setReturnValueHandlers(returnValueHandlerManager);
    return handler;
  }

  public ActionMappingAnnotationHandler create(
          String beanName, Method method, @Nullable Class<?> beanType, @Nullable List<HandlerInterceptor> interceptors) {
    Assert.state(returnValueHandlerManager != null, "No ReturnValueHandlers set");
    Assert.state(parameterFactory != null, "No ResolvableParameterFactory set");

    if (beanType == null) {
      beanType = beanFactory.getType(beanName);
      if (beanType != null) {
        beanType = ClassUtils.getUserClass(beanType);
      }
    }
    ActionMappingAnnotationHandler handler;
    if (beanFactory.isSingleton(beanName)) {
      Object bean = beanFactory.getBean(beanName);
      handler = ActionMappingAnnotationHandler.from(bean, method, parameterFactory, beanType);
    }
    else {
      BeanSupplier<Object> beanSupplier = BeanSupplier.from(beanFactory, beanName);
      handler = ActionMappingAnnotationHandler.from(beanSupplier, method, parameterFactory, beanType);
    }
    handler.setInterceptors(interceptors);
    handler.setReturnValueHandlers(returnValueHandlerManager);
    return handler;
  }

  public void setReturnValueHandlerManager(@Nullable ReturnValueHandlerManager manager) {
    this.returnValueHandlerManager = manager;
  }

  public void setParameterFactory(ResolvableParameterFactory parameterFactory) {
    this.parameterFactory = parameterFactory;
  }

  public void setParameterResolvingRegistry(@Nullable ParameterResolvingRegistry registry) {
    this.parameterFactory = new ParameterResolvingRegistryResolvableParameterFactory(registry);
  }

}
