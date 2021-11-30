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

import java.io.IOException;

import cn.taketoday.aop.support.annotation.BeanSupplier;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.view.ReturnValueHandler;
import cn.taketoday.web.view.ReturnValueHandlers;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/29 22:48</a>
 * @see cn.taketoday.web.annotation.RequestMapping
 * @see cn.taketoday.web.annotation.Controller
 * @since 4.0
 */
public class AnnotationHandlerMethod
        extends InterceptableRequestHandler implements HandlerAdapter, ReturnValueHandler {
  private final HandlerMethod handlerMethod;
  private final BeanSupplier<Object> beanSupplier;

  // handler fast invoker
  private volatile MethodInvoker handlerInvoker;

  // return-value handlers(registry)
  private ReturnValueHandlers resultHandlers;

  // target return-value handler
  private ReturnValueHandler returnValueHandler;

  public AnnotationHandlerMethod(BeanSupplier<Object> beanSupplier, HandlerMethod handlerMethod) {
    this.beanSupplier = beanSupplier;
    this.handlerMethod = handlerMethod;
  }

  public HandlerMethod getHandlerMethod() {
    return handlerMethod;
  }

  public MethodInvoker getHandlerInvoker() {
    return handlerInvoker;
  }

  public void setResultHandlers(ReturnValueHandlers resultHandlers) {
    this.resultHandlers = resultHandlers;
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

    MethodParameter[] parameters = handlerMethod.getParameters();
    if (ObjectUtils.isEmpty(parameters)) {
      return handlerInvoker.invoke(beanSupplier.get(), null);
    }
    Object[] args = new Object[parameters.length];
    int i = 0;
    for (MethodParameter parameter : parameters) {
      args[i++] = parameter.resolveParameter(context);
    }
    return handlerInvoker.invoke(beanSupplier.get(), args);
  }

  // HandlerAdapter

  @Override
  public boolean supports(Object handler) {
    return handler == this;
  }

  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    return handleRequest(context);
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

  @Override
  public boolean supportsHandler(Object handler) {
    return handler == this;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws IOException {
    applyResponseStatus(context);
    if (returnValueHandler == null) {
      returnValueHandler = resultHandlers.obtainHandler(this);
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

  static class SingletonAnnotationHandlerMethod extends AnnotationHandlerMethod {

    public SingletonAnnotationHandlerMethod(BeanSupplier<Object> beanSupplier, HandlerMethod handlerMethod) {
      super(beanSupplier, handlerMethod);
    }
  }

}
