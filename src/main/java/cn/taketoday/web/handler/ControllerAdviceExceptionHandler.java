/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.exception.ExceptionUnhandledException;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 * 2019-06-22 19:17
 * @since 2.3.7
 */
public class ControllerAdviceExceptionHandler
        extends DefaultExceptionHandler implements WebApplicationInitializer {

  private static final Logger log = LoggerFactory.getLogger(ControllerAdviceExceptionHandler.class);
  private final Map<Class<? extends Throwable>, ThrowableHandlerMethod> exceptionHandlers = new HashMap<>();

  @Override
  protected Object handleHandlerMethodInternal(final Throwable ex,
                                               final RequestContext context,
                                               final HandlerMethod handlerMethod) throws Throwable //
  {
    final ThrowableHandlerMethod exceptionHandler = lookupExceptionHandler(ex);
    if (exceptionHandler == null) {
      return super.handleHandlerMethodInternal(ex, context, handlerMethod);
    }

    context.attribute(Constant.KEY_THROWABLE, ex);
    try {
      exceptionHandler.handleResult(context,
                                    exceptionHandler,
                                    exceptionHandler.invokeHandler(context));
    }
    catch (final Throwable target) {
      if (!(target instanceof ExceptionUnhandledException)) {
        log.error("Handling of [{}] resulted in Exception: [{}]", //
                  target.getClass().getName(), target.getClass().getName(), target);
        throw target;
      }
    }
    return NONE_RETURN_VALUE;
  }

  /**
   * Looking for exception handler mapping
   *
   * @param ex
   *         Target {@link Exception}
   *
   * @return Mapped {@link Exception} handler mapping
   */
  protected ThrowableHandlerMethod lookupExceptionHandler(final Throwable ex) {
    final ThrowableHandlerMethod ret = exceptionHandlers.get(ex.getClass());
    if (ret == null) {
      return exceptionHandlers.get(Throwable.class); // Global exception handler
    }
    return ret;
  }

  @Override
  public void onStartup(WebApplicationContext beanFactory) {
    log.info("Initialize controller advice exception resolver");
    // get all error handlers
    final List<Object> errorHandlers = beanFactory.getAnnotatedBeans(ControllerAdvice.class);
    for (final Object errorHandler : errorHandlers) {

      for (final Method method : ReflectionUtils.getDeclaredMethods(errorHandler.getClass())) {
        if (method.isAnnotationPresent(ExceptionHandler.class)) {

          for (Class<? extends Throwable> exceptionClass : method.getAnnotation(ExceptionHandler.class).value()) {
            final ThrowableHandlerMethod handler = new ThrowableHandlerMethod(errorHandler, method);
            if (handler.getResponseStatus() == null) {
              handler.setResponseStatus(WebUtils.getResponseStatus(exceptionClass));
            }
            exceptionHandlers.put(exceptionClass, handler);
          }
        }
      }
    }
  }

  protected static class ThrowableHandlerMethod extends HandlerMethod {

    public ThrowableHandlerMethod(Object handler, Method method) {
      super(handler, method, null);
    }

    @Override
    protected void applyResponseStatus(final RequestContext context) throws IOException {
      final ResponseStatus status = getResponseStatus();
      if (status == null) {
        final Object attribute = context.attribute(Constant.KEY_THROWABLE);
        if (attribute instanceof Throwable) {
          final ResponseStatus runtimeErrorStatus = WebUtils.getResponseStatus((Throwable) attribute);
          applyResponseStatus(context, runtimeErrorStatus);
        }
      }
      else {
        super.applyResponseStatus(context);
      }
    }
  }

}
