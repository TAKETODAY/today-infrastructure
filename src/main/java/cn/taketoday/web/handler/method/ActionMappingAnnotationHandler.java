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
import java.util.Objects;

import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.InterceptableRequestHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;

/**
 * HTTP Request Annotation Handler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.web.annotation.RequestMapping
 * @see cn.taketoday.web.annotation.ActionMapping
 * @see cn.taketoday.web.annotation.Controller
 * @since 4.0 2021/11/29 22:48
 */
public abstract class ActionMappingAnnotationHandler
        extends InterceptableRequestHandler implements HandlerAdapter {
  private final HandlerMethod handlerMethod;

  // handler fast invoker
  private /*volatile*/ MethodInvoker handlerInvoker;

  // return-value handlers(registry)
  private ReturnValueHandlerManager returnValueHandlerManager;

  // target return-value handler
  private ReturnValueHandler returnValueHandler;

  // resolvable parameters
  @Nullable
  private final ResolvableMethodParameter[] resolvableParameters;
  private final Class<?> beanType;

  public ActionMappingAnnotationHandler(
          HandlerMethod handlerMethod, @Nullable ResolvableMethodParameter[] parameters, Class<?> beanType) {
    this.resolvableParameters = parameters;
    this.handlerMethod = handlerMethod;
    this.beanType = beanType;
  }

  public ActionMappingAnnotationHandler(ActionMappingAnnotationHandler handler) {
    this.beanType = handler.beanType;
    this.handlerMethod = handler.handlerMethod;
    this.handlerInvoker = handler.handlerInvoker;
    this.returnValueHandlerManager = handler.returnValueHandlerManager;
    this.returnValueHandler = handler.returnValueHandler;
    this.resolvableParameters = handler.resolvableParameters;
  }

  @Nullable
  public ResolvableMethodParameter[] getResolvableParameters() {
    return resolvableParameters;
  }

  public Method getJavaMethod() {
    return handlerMethod.getMethod();
  }

  public HandlerMethod getMethod() {
    return handlerMethod;
  }

  public void setReturnValueHandlers(ReturnValueHandlerManager resultHandlers) {
    this.returnValueHandlerManager = resultHandlers;
  }

  // InterceptableRequestHandler

  @Override
  protected Object handleInternal(RequestContext context) throws Throwable {
    MethodInvoker handlerInvoker = this.handlerInvoker;
    if (handlerInvoker == null) {
      synchronized(this) {
        handlerInvoker = this.handlerInvoker;
        if (handlerInvoker == null) {
          handlerInvoker = MethodInvoker.fromMethod(handlerMethod.getMethod());
          this.handlerInvoker = handlerInvoker;
        }
      }
    }

    Object handlerBean = getHandlerObject();
    ResolvableMethodParameter[] parameters = getResolvableParameters();
    if (ObjectUtils.isEmpty(parameters)) {
      return handlerInvoker.invoke(handlerBean, null);
    }
    Object[] args = new Object[parameters.length];
    int i = 0;
    for (ResolvableMethodParameter parameter : parameters) {
      args[i++] = parameter.resolveParameter(context);
    }

    return handlerInvoker.invoke(handlerBean, args);
  }

  public abstract Object getHandlerObject();

  public Class<?> getBeanType() {
    return beanType;
  }

  // HandlerAdapter

  @Override
  public boolean supports(Object handler) {
    return handler == this;
  }

  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    Object returnValue = handleRequest(context);
    handleReturnValue(context, handler, returnValue);
    return NONE_RETURN_VALUE;
  }

  // ReturnValueHandler

  /**
   * Set the response status according to the {@link ResponseStatus} annotation.
   */
  protected void applyResponseStatus(RequestContext context) {
    applyResponseStatus(context, handlerMethod.getResponseStatus());
  }

  protected void applyResponseStatus(RequestContext context, ResponseStatus status) {
    if (status != null) {
      String reason = status.reason();
      HttpStatus httpStatus = status.value();
      if (StringUtils.hasText(reason)) {
        context.setStatus(httpStatus.value(), reason);
      }
      else {
        context.setStatus(httpStatus);
      }
    }
  }

  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws Exception {
    applyResponseStatus(context);

    ReturnValueHandler returnValueHandler = this.returnValueHandler;
    if (returnValueHandler == null) {
      returnValueHandler = returnValueHandlerManager.obtainHandler(this);
      this.returnValueHandler = returnValueHandler;
    }
    returnValueHandler.handleReturnValue(context, handler, returnValue);
    // @since 3.0
    String contentType = handlerMethod.getContentType();
    if (contentType != null) {
      context.setContentType(contentType);
    }
  }

  public Object invokeHandler(RequestContext request) throws Throwable {
    return handleInternal(request);
  }

  @Override
  public String toString() {
    return handlerMethod.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ActionMappingAnnotationHandler that))
      return false;
    return Objects.equals(handlerMethod, that.handlerMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(handlerMethod);
  }

  //---------------------------------------------------------------------
  // Static methods
  //---------------------------------------------------------------------

  public static ActionMappingAnnotationHandler from(
          Object handlerBean, Method method, ResolvableParameterFactory parameterFactory, Class<?> beanType) {
    HandlerMethod handlerMethod = HandlerMethod.from(method);
    ResolvableMethodParameter[] parameters = parameterFactory.createArray(handlerMethod);
    return new SingletonActionMappingAnnotationHandler(handlerBean, handlerMethod, parameters, beanType);
  }

  public static ActionMappingAnnotationHandler from(
          BeanSupplier<Object> beanSupplier, Method method, ResolvableParameterFactory parameterFactory, Class<?> beanType) {
    HandlerMethod handlerMethod = HandlerMethod.from(method);
    ResolvableMethodParameter[] parameters = parameterFactory.createArray(handlerMethod);
    return new SuppliedActionMappingAnnotationHandler(beanSupplier, handlerMethod, parameters, beanType);
  }

}
