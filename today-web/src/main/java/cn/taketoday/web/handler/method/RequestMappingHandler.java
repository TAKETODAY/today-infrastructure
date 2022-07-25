/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import cn.taketoday.context.MessageSource;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.HttpStatusCodeProvider;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.MethodInvoker;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.InterceptableRequestHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.ReturnValueHandlerNotFoundException;
import cn.taketoday.web.util.WebUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/25 22:00
 */
public class RequestMappingHandler extends InterceptableRequestHandler {
  private final ServletInvocableHandlerMethod handlerMethod;

  // handler fast invoker
  private /*volatile*/ MethodInvoker handlerInvoker;

  // return-value handlers(registry)
  private final ReturnValueHandlerManager returnValueHandlerManager;

  // target return-value handler
  private ReturnValueHandler returnValueHandler;

  public RequestMappingHandler(ServletInvocableHandlerMethod handlerMethod, ReturnValueHandlerManager manager) {
    this.handlerMethod = handlerMethod;
    this.returnValueHandlerManager = manager;
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

    Object handlerBean = handlerMethod.getBean();
    ResolvableMethodParameter[] parameters = handlerMethod.resolvableParameters;
    if (parameters.length == 0) {
      return handlerInvoker.invoke(handlerBean, null);
    }
    Object[] args = new Object[parameters.length];
    int i = 0;
    for (ResolvableMethodParameter parameter : parameters) {
      args[i++] = parameter.resolveParameter(context);
    }

    return handlerInvoker.invoke(handlerBean, args);
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
    Object handlerBean = handlerMethod.getBean();

    ResolvableMethodParameter[] parameters = handlerMethod.resolvableParameters;
    if (parameters == null) {
      return handlerInvoker.invoke(handlerBean, null);
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
    return handlerInvoker.invoke(handlerBean, args);
  }

  @Nullable
  protected static Object findProvidedArgument(
          ResolvableMethodParameter parameter, @Nullable Object[] providedArgs) {
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

  protected void applyResponseStatus(RequestContext context, HttpStatusCode status) {
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

  public void handleReturnValue(RequestContext context, Object returnValue) throws Exception {
    applyResponseStatus(context);

    ReturnValueHandler returnValueHandler = this.returnValueHandler;
    if (returnValueHandler == null) {
      returnValueHandler = returnValueHandlerManager.findHandler(handlerMethod, returnValue);
      if (returnValueHandler == null) {
        throw new ReturnValueHandlerNotFoundException(returnValue, this);
      }
      this.returnValueHandler = returnValueHandler;
    }

    returnValueHandler.handleReturnValue(context, handlerMethod, returnValue);
    // @since 3.0
    String contentType = handlerMethod.getContentType();
    if (contentType != null) {
      context.setContentType(contentType);
    }
  }

  @Override
  public String toString() {
    return handlerMethod.toString();
  }

}
