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
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.context.MessageSource;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.HttpStatusCodeProvider;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.MethodInvoker;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.InterceptableRequestHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.util.WebUtils;

/**
 * HTTP Request Annotation Handler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.web.annotation.RequestMapping
 * @see cn.taketoday.web.annotation.ActionMapping
 * @see cn.taketoday.stereotype.Controller
 * @since 4.0 2021/11/29 22:48
 */
public abstract class ActionMappingAnnotationHandler extends InterceptableRequestHandler {
  private final HandlerMethod handlerMethod;

  // handler fast invoker
  @Nullable
  private /*volatile*/ MethodInvoker handlerInvoker;

  // return-value handlers(registry)
  @Nullable
  private ReturnValueHandlerManager returnValueHandlerManager;

  // target return-value handler
  @Nullable
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
    Object returnValue = doInvoke(context);
    handleReturnValue(context, handlerMethod, returnValue);
    return NONE_RETURN_VALUE;
  }

  private Object doInvoke(RequestContext context) throws Throwable {
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

  protected Object invoke(RequestContext context, Object... providedArgs) throws Throwable {
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

    ResolvableMethodParameter[] parameters = getResolvableParameters();
    if (parameters == null) {
      return handlerInvoker.invoke(getHandlerObject(), null);
    }

    Object[] args = new Object[parameters.length];
    int i = 0;
    for (ResolvableMethodParameter resolvable : parameters) {
      Object argument = findProvidedArgument(resolvable, providedArgs);
      if (argument == null) {
        argument = resolvable.resolveParameter(context);
      }
      args[i++] = argument;
    }
    return handlerInvoker.invoke(getHandlerObject(), args);
  }

  @Nullable
  protected static Object findProvidedArgument(
          ResolvableMethodParameter parameter, @Nullable Object... providedArgs) {
    if (ObjectUtils.isNotEmpty(providedArgs)) {
      Class<?> parameterType = parameter.getParameterType();
      for (Object providedArg : providedArgs) {
        if (parameterType.isInstance(providedArg)) {
          return providedArg;
        }
      }
    }
    return null;
  }

  // ReturnValueHandler

  /**
   * Set the response status according to the {@link ResponseStatus} annotation.
   */
  protected void applyResponseStatus(RequestContext context) {
    applyResponseStatus(context, handlerMethod.getResponseStatus());
  }

  protected void applyResponseStatus(RequestContext context, @Nullable HttpStatusCode status) {
    if (status != null) {
      String reason = handlerMethod.getResponseStatusReason();
      int httpStatus = status.value();
      if (StringUtils.hasText(reason)) {
        MessageSource messageSource = context.getApplicationContext();
        String message = messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale());
        context.setStatus(httpStatus, message);
      }
      else {
        context.setStatus(httpStatus);
      }
    }
    else {
      Object attribute = context.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
      if (attribute instanceof HttpStatusCodeProvider provider) { // @since 3.0.1
        HttpStatusCode httpStatus = provider.getStatusCode();
        context.setStatus(httpStatus);
      }
      else if (attribute instanceof Throwable throwable) {
        ResponseStatus runtimeErrorStatus = HandlerMethod.getResponseStatus(throwable);
        applyResponseStatus(context, runtimeErrorStatus.code());
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
          Object handlerBean, Method method, ResolvableParameterFactory parameterFactory) {
    return from(handlerBean, method, parameterFactory, ClassUtils.getUserClass(handlerBean));
  }

  public static ActionMappingAnnotationHandler from(
          Object handlerBean, Method method, ResolvableParameterFactory parameterFactory, Class<?> beanType) {
    HandlerMethod handlerMethod = new HandlerMethod(handlerBean, method);
    ResolvableMethodParameter[] parameters = parameterFactory.createArray(handlerMethod);
    return new SingletonActionMappingAnnotationHandler(handlerBean, handlerMethod, parameters, beanType);
  }

  public static ActionMappingAnnotationHandler from(
          Supplier<Object> beanSupplier, Method method, ResolvableParameterFactory parameterFactory, Class<?> beanType) {
    HandlerMethod handlerMethod;
    if (beanSupplier instanceof BeanSupplier<Object> supplier) {
      handlerMethod = new HandlerMethod(supplier.getBeanName(), supplier.getBeanFactory(), method);
    }
    else {
      handlerMethod = new HandlerMethod(beanSupplier.get(), method);
    }
    ResolvableMethodParameter[] parameters = parameterFactory.createArray(handlerMethod);
    return new SuppliedActionMappingAnnotationHandler(beanSupplier, handlerMethod, parameters, beanType);
  }

}
