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

import java.io.IOException;
import java.lang.reflect.Method;

import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.ReturnValueHandlerNotFoundException;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.view.View;

/**
 * Extends {@link InvocableHandlerMethod} with the ability to handle return
 * values through a registered {@link HandlerMethodReturnValueHandler} and
 * also supports setting the response status based on a method-level
 * {@code @ResponseStatus} annotation.
 *
 * <p>A {@code null} return value (including void) may be interpreted as the
 * end of request processing in combination with a {@code @ResponseStatus}
 * annotation, a not-modified check condition
 * (see {@link cn.taketoday.web.RequestContext#checkNotModified(long)}), or
 * a method argument that provides access to the response stream.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:50
 */
public class ResultableHandlerMethod extends InvocableHandlerMethod {

  private final ReturnValueHandlerManager returnValueHandlerManager;

  /**
   * Creates an instance from the given handler and method.
   */
  public ResultableHandlerMethod(Object handler,
          Method method, ResolvableParameterFactory factory,
          ReturnValueHandlerManager returnValueHandlerManager) {
    super(handler, method, factory);
    this.returnValueHandlerManager = returnValueHandlerManager;
  }

  /**
   * Create an instance from a {@code HandlerMethod}.
   */
  public ResultableHandlerMethod(HandlerMethod handlerMethod,
          ReturnValueHandlerManager manager, ResolvableParameterFactory factory) {
    super(handlerMethod, factory);
    this.returnValueHandlerManager = manager;
  }

  /**
   * Invoke the method and handle the return value through one of the
   * configured {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}.
   *
   * @param request the current request
   * @param bindingContext the binding context to use
   * @param providedArgs "given" arguments matched by type (not resolved)
   */
  public Object invokeAndHandle(
          RequestContext request, BindingContext bindingContext, Object... providedArgs) throws Throwable {
    request.setBindingContext(bindingContext);

    Object returnValue = invokeForRequest(request, bindingContext, providedArgs);
    applyResponseStatus(request);

    if (returnValue == null) {
      if (isRequestNotModified(request) || getResponseStatus() != null) {
        return HttpRequestHandler.NONE_RETURN_VALUE;
      }
    }
    else if (StringUtils.hasText(getResponseStatusReason())) {
      return HttpRequestHandler.NONE_RETURN_VALUE;
    }

    ReturnValueHandler returnValueHandler = returnValueHandlerManager.findHandler(this, returnValue);
    if (returnValueHandler == null) {
      returnValueHandler = returnValueHandlerManager.getByReturnValue(returnValue);
    }

    if (returnValueHandler == null) {
      throw new ReturnValueHandlerNotFoundException(returnValue, this);
    }

    try {
      returnValueHandler.handleReturnValue(request, this, returnValue);
      return HttpRequestHandler.NONE_RETURN_VALUE;
    }
    catch (Exception ex) {
      if (log.isTraceEnabled()) {
        log.trace(formatErrorForReturnValue(returnValue), ex);
      }
      throw ex;
    }
  }

  /**
   * Set the response status according to the {@link ResponseStatus} annotation.
   */
  private void applyResponseStatus(RequestContext response) throws IOException {
    HttpStatusCode status = getResponseStatus();
    if (status == null) {
      return;
    }

    String reason = getResponseStatusReason();
    if (StringUtils.hasText(reason)) {
      response.sendError(status.value(), reason);
    }
    else {
      response.setStatus(status.value());
    }

    // To be picked up by RedirectView
    response.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
  }

  /**
   * Does the given request qualify as "not modified"?
   *
   * @see RequestContext#checkNotModified(long)
   * @see RequestContext#checkNotModified(String)
   */
  private boolean isRequestNotModified(RequestContext webRequest) {
    return webRequest.isNotModified();
  }

  private String formatErrorForReturnValue(@Nullable Object returnValue) {
    return "Error handling return value=[" + returnValue + "]" +
            (returnValue != null ? ", type=" + returnValue.getClass().getName() : "") +
            " in " + this;
  }

}
