/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.utils.WebUtils;

/**
 * Handle {@link ExceptionHandler}
 *
 * @author TODAY <br>
 * 2019-06-22 19:17
 * @since 2.3.7
 */
public class DefaultExceptionHandler
        extends SimpleExceptionHandler implements WebApplicationInitializer {

  private static final Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);
  private final HashMap<Class<? extends Throwable>, ThrowableHandlerMethod>
          exceptionHandlers = new HashMap<>();

  /** @since 3.0 */
  private boolean inheritable;

  @Override
  public Object handleException(RequestContext context, Throwable target, Object handler) throws Throwable {
    // prepare context throwable
    context.attribute(Constant.KEY_THROWABLE, target);
    // catch all handlers
    final ThrowableHandlerMethod exHandler = lookupExceptionHandler(target);
    if (exHandler == null) {
      return super.handleException(context, target, handler);
    }

    logCatchThrowable(target);
    try {
      return handleException(context, exHandler);
    }
    catch (final Throwable handlerEx) {
      logResultedInException(target, handlerEx);
      throw target;
    }
  }

  /**
   * Handle Exception use {@link ThrowableHandlerMethod}
   *
   * @param context
   *         current request
   * @param exHandler
   *         ThrowableHandlerMethod
   *
   * @return handler return value
   *
   * @throws Throwable
   *         Throwable occurred in exHandler
   */
  protected Object handleException(RequestContext context, ThrowableHandlerMethod exHandler)
          throws Throwable {
    exHandler.handleResult(context, exHandler, exHandler.invokeHandler(context));
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
      if (inheritable) {
        final Class<? extends Throwable> runtimeEx = ex.getClass();
        for (final Map.Entry<Class<? extends Throwable>, ThrowableHandlerMethod> entry
                : exceptionHandlers.entrySet()) {
          final Class<? extends Throwable> entryKey = entry.getKey();

          if (entryKey.isAssignableFrom(runtimeEx)) {
            return entry.getValue();
          }
        }
      }
      return exceptionHandlers.get(Throwable.class); // Global exception handler
    }
    return ret;
  }

  //

  /**
   * Set if handler is inheritable ?
   *
   * @param inheritable
   *         is inheritable
   */
  public void setInheritable(boolean inheritable) {
    this.inheritable = inheritable;
  }

  // WebApplicationInitializer

  @Override
  public void onStartup(WebApplicationContext beanFactory) {
    log.info("Initialize @ExceptionHandler");
    // get all error handlers
    final List<Object> errorHandlers = beanFactory.getAnnotatedBeans(ControllerAdvice.class);
    for (final Object errorHandler : errorHandlers) {
      for (final Method method : ReflectionUtils.getDeclaredMethods(errorHandler.getClass())) {
        if (method.isAnnotationPresent(ExceptionHandler.class)) {
          final Class<? extends Throwable>[] catchExClasses = getCatchThrowableClasses(method);
          for (Class<? extends Throwable> exceptionClass : catchExClasses) {
            exceptionHandlers.put(exceptionClass, new ThrowableHandlerMethod(errorHandler, method));
          }
        }
      }
    }
  }

  /**
   * @param method
   *         target handler method
   *
   * @return Throwable class array
   */
  protected Class<? extends Throwable>[] getCatchThrowableClasses(final Method method) {
    Class<? extends Throwable>[] catchExClasses = method.getAnnotation(ExceptionHandler.class).value();
    if (ObjectUtils.isEmpty(catchExClasses)) {
      final Class<?>[] parameterTypes = method.getParameterTypes();
      if (ObjectUtils.isEmpty(parameterTypes)) {
        catchExClasses = new Class[] { Throwable.class };
      }
      else {
        HashSet<Class<?>> classes = new HashSet<>();
        for (final Class<?> parameterType : parameterTypes) {
          if (Throwable.class.isAssignableFrom(parameterType)) {
            classes.add(parameterType);
          }
        }
        catchExClasses = classes.toArray(new Class[classes.size()]);
      }
    }
    return catchExClasses;
  }

  // exception handler

  protected static class ThrowableHandlerMethod extends HandlerMethod {

    public ThrowableHandlerMethod(Object handler, Method method) {
      super(handler, method, null);
    }

    @Override
    protected void applyResponseStatus(final RequestContext context) {
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
