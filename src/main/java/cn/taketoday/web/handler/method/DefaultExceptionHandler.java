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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCapable;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.SimpleExceptionHandler;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.util.WebUtils;

/**
 * Handle {@link ExceptionHandler} annotated method
 * <p>
 * this method indicates that is a exception handler
 * </p>
 *
 * @author TODAY 2019-06-22 19:17
 * @since 2.3.7
 */
public class DefaultExceptionHandler
        extends SimpleExceptionHandler implements WebApplicationInitializer {

  private static final Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);
  private final HashMap<Class<? extends Throwable>, ExceptionHandlerMappingHandler>
          exceptionHandlers = new HashMap<>();

  /** @since 3.0 */
  private boolean inheritable;

  /** @since 3.0 */
  private ExceptionHandlerMappingHandler globalHandler;

  @Override
  public Object handleException(RequestContext context, Throwable target, Object handler) {
    // catch all handlers
    ExceptionHandlerMappingHandler exHandler = lookupExceptionHandler(target);
    if (exHandler == null) {
      return super.handleException(context, target, handler);
    }

    logCatchThrowable(target);
    try {
      if (log.isDebugEnabled()) {
        log.debug("Using @ExceptionHandler {}", exHandler);
      }
      return handleException(context, exHandler);
    }
    catch (Throwable handlerEx) {
      logResultedInException(target, handlerEx);
      // next handler
      return null;
    }
  }

  /**
   * Handle Exception use {@link ExceptionHandlerMappingHandler}
   *
   * @param context current request
   * @param exHandler ThrowableHandlerMethod
   * @return handler return value
   * @throws Throwable occurred in exHandler
   */
  protected Object handleException(RequestContext context, ExceptionHandlerMappingHandler exHandler)
          throws Throwable {
    exHandler.handleReturnValue(context, exHandler, exHandler.invokeHandler(context));
    return NONE_RETURN_VALUE;
  }

  /**
   * Looking for exception handler mapping
   *
   * @param ex Target {@link Exception}
   * @return Mapped {@link Exception} handler mapping
   */
  protected ExceptionHandlerMappingHandler lookupExceptionHandler(Throwable ex) {
    ExceptionHandlerMappingHandler ret = exceptionHandlers.get(ex.getClass());
    if (ret == null) {
      if (inheritable) {
        Class<? extends Throwable> runtimeEx = ex.getClass();
        for (var entry : exceptionHandlers.entrySet()) {
          Class<? extends Throwable> entryKey = entry.getKey();
          if (entryKey != Throwable.class && entryKey.isAssignableFrom(runtimeEx)) {
            return entry.getValue();
          }
        }
      }
      return globalHandler; // Global exception handler
    }
    return ret;
  }

  //

  /**
   * Set if handler is inheritable ?
   *
   * @param inheritable is inheritable
   */
  public void setInheritable(boolean inheritable) {
    this.inheritable = inheritable;
  }

  void setGlobalHandler(ExceptionHandlerMappingHandler globalHandler) {
    this.globalHandler = globalHandler;
  }

  // WebApplicationInitializer

  @Override
  public void onStartup(WebApplicationContext context) {
    log.info("Initialize @ExceptionHandler");
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    ParameterResolvingRegistry registry = beanFactory.getBean(ParameterResolvingRegistry.class);
    var parameterFactory = new ParameterResolvingRegistryResolvableParameterFactory(registry);
    ReturnValueHandlerManager manager = beanFactory.getBean(ReturnValueHandlerManager.class);

    Set<String> errorHandlers = beanFactory.getBeanNamesForAnnotation(ControllerAdvice.class);
    // get all error handlers
    for (String errorHandler : errorHandlers) {
      Class<?> errorHandlerType = beanFactory.getType(errorHandler);
      for (Method method : ReflectionUtils.getDeclaredMethods(errorHandlerType)) {
        if (method.isAnnotationPresent(ExceptionHandler.class)) {
          for (var exceptionType : getCatchThrowableClasses(method)) {
            // @since 3.0
            BeanSupplier<Object> handlerBean = BeanSupplier.from(beanFactory, errorHandler);
            ExceptionHandlerMappingHandler handler = getHandler(
                    handlerBean, parameterFactory, method, manager);

            ExceptionHandlerMappingHandler oldHandler = exceptionHandlers.put(exceptionType, handler);
            if (oldHandler != null && !method.equals(oldHandler.getJavaMethod())) {
              throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
                      exceptionType + "]: {" + oldHandler.getJavaMethod() + ", " + method + "}");
            }
          }
        }
      }
    }

    // @since 3.0
    ExceptionHandlerMappingHandler global = exceptionHandlers.get(Throwable.class);
    if (global != null) {
      setGlobalHandler(global);
      exceptionHandlers.remove(Throwable.class);
    }
  }

  private ExceptionHandlerMappingHandler getHandler(
          BeanSupplier<Object> handlerBean, ResolvableParameterFactory parameterFactory,
          Method method, ReturnValueHandlerManager manager) {
    HandlerMethod handlerMethod = HandlerMethod.from(method);
    ExceptionHandlerMappingHandler handler = new ExceptionHandlerMappingHandler(handlerBean, handlerMethod, parameterFactory.createArray(method));
    handler.setReturnValueHandlers(manager);
    return handler;
  }

  /**
   * @param method target handler method
   * @return Throwable class array
   */
  @SuppressWarnings("unchecked")
  protected Class<? extends Throwable>[] getCatchThrowableClasses(Method method) {
    Class<? extends Throwable>[] catchExClasses = method.getAnnotation(ExceptionHandler.class).value();
    if (ObjectUtils.isEmpty(catchExClasses)) {
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (ObjectUtils.isEmpty(parameterTypes)) {
        catchExClasses = new Class[] { Throwable.class };
      }
      else {
        HashSet<Class<?>> classes = new HashSet<>();
        for (Class<?> parameterType : parameterTypes) {
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

  protected static class ExceptionHandlerMappingHandler extends SuppliedActionMappingAnnotationHandler {

    ExceptionHandlerMappingHandler(
            BeanSupplier<Object> beanSupplier, HandlerMethod method, @Nullable ResolvableMethodParameter[] parameters) {
      super(beanSupplier, method, parameters);
    }

    @Override
    protected void applyResponseStatus(RequestContext context) {
      ResponseStatus status = getMethod().getResponseStatus();
      if (status == null) {
        Object attribute = context.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
        if (attribute instanceof HttpStatusCapable) { // @since 3.0.1
          HttpStatus httpStatus = ((HttpStatusCapable) attribute).getStatus();
          context.setStatus(httpStatus);
        }
        else if (attribute instanceof Throwable) {
          ResponseStatus runtimeErrorStatus = HandlerMethod.getResponseStatus((Throwable) attribute);
          applyResponseStatus(context, runtimeErrorStatus);
        }
      }
      else {
        // Annotated with @ResponseStatus
        super.applyResponseStatus(context, status);
      }
    }

  }

}
