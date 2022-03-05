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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.handler.AbstractActionMappingMethodExceptionHandler;

/**
 * Handle {@link ExceptionHandler} annotated method
 * <p>
 * this method indicates that is a exception handler
 * </p>
 *
 * @author TODAY 2019-06-22 19:17
 * @since 2.3.7
 */
public class ExceptionHandlerAnnotationExceptionHandler
        extends AbstractActionMappingMethodExceptionHandler implements ApplicationContextAware, InitializingBean {

  private final ConcurrentHashMap<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
          new ConcurrentHashMap<>(64);

  private final LinkedHashMap<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
          new LinkedHashMap<>();

  // TODO optimise
  private final ConcurrentHashMap<Method, ActionMappingAnnotationHandler> exceptionHandlerMapping =
          new ConcurrentHashMap<>(64);

  @Nullable
  private ApplicationContext applicationContext;

  private AnnotationHandlerFactory handlerFactory;

  @Nullable
  @Override
  protected Object handleInternal(RequestContext context,
          @Nullable ActionMappingAnnotationHandler annotationHandler, Throwable target) {
    // catch all handlers
    var exHandler = lookupExceptionHandler(annotationHandler, target);
    if (exHandler == null) {
      return null; // next
    }

    logCatchThrowable(target);
    ArrayList<Throwable> exceptions = new ArrayList<>();
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Using @ExceptionHandler {}", exHandler);
      }
      // Expose causes as provided arguments as well
      Throwable exToExpose = target;
      while (exToExpose != null) {
        exceptions.add(exToExpose);
        Throwable cause = exToExpose.getCause();
        exToExpose = cause != exToExpose ? cause : null;
      }

      // efficient arraycopy call in ArrayList
      Object[] arguments = exceptions.toArray(new Object[exceptions.size() + 2]);
      if (annotationHandler != null) {
        arguments[arguments.length - 1] = annotationHandler;
        arguments[arguments.length - 2] = annotationHandler.getMethod();
      }

      Object returnValue = exHandler.invoke(context, arguments);
      exHandler.handleReturnValue(context, exHandler, returnValue);
      return NONE_RETURN_VALUE;
    }
    catch (Throwable invocationEx) {
//      logResultedInException(target, invocationEx);
      // Any other than the original exception (or a cause) is unintended here,
      // probably an accident (e.g. failed assertion or the like).
      if (!exceptions.contains(invocationEx) && logger.isWarnEnabled()) {
        logger.warn("Failure in @ExceptionHandler {}", exHandler, invocationEx);
      }
      // Continue with default processing of the original exception...
      return null;
    }
  }

  /**
   * Find an {@code @ExceptionHandler} method for the given exception. The default
   * implementation searches methods in the class hierarchy of the controller first
   * and if not found, it continues searching for additional {@code @ExceptionHandler}
   * methods assuming some {@linkplain ControllerAdvice @ControllerAdvice}
   * Spring-managed beans were detected.
   *
   * @param exception the raised exception
   * @return a method to handle the exception, or {@code null} if none
   */
  @Nullable
  protected ActionMappingAnnotationHandler lookupExceptionHandler(
          @Nullable ActionMappingAnnotationHandler annotationHandler, Throwable exception) {

    Class<?> handlerType = null;

    if (annotationHandler != null) {
      // Local exception handler methods on the controller class itself.
      // To be invoked through the proxy, even in case of an interface-based proxy.
      handlerType = annotationHandler.getBeanType();
      var resolver = exceptionHandlerCache.computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new);
      Method method = resolver.resolveMethod(exception);
      if (method != null) {
        return exceptionHandlerMapping.computeIfAbsent(method,
                key -> getHandler(annotationHandler::getHandlerObject, key, annotationHandler.getBeanType()));
      }
      // For advice applicability check below (involving base packages, assignable types
      // and annotation presence), use target class instead of interface-based proxy.
      if (Proxy.isProxyClass(handlerType)) {
        handlerType = AopUtils.getTargetClass(annotationHandler.getHandlerObject());
      }
    }

    for (var entry : exceptionHandlerAdviceCache.entrySet()) {
      ControllerAdviceBean advice = entry.getKey();
      if (advice.isApplicableToBeanType(handlerType)) {
        ExceptionHandlerMethodResolver resolver = entry.getValue();
        Method method = resolver.resolveMethod(exception);
        if (method != null) {
          return exceptionHandlerMapping.computeIfAbsent(method,
                  key -> getHandler(advice::resolveBean, key, advice.getBeanType()));
        }
      }
    }

    return null;
  }

  private ActionMappingAnnotationHandler getHandler(
          Supplier<Object> handlerBean, Method method, Class<?> errorHandlerType) {
    return handlerFactory.create(handlerBean, method, errorHandlerType, null);
  }

  @Override
  protected boolean hasGlobalExceptionHandlers() {
    return !this.exceptionHandlerAdviceCache.isEmpty();
  }

  //

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Nullable
  public ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  public void setHandlerFactory(AnnotationHandlerFactory handlerFactory) {
    this.handlerFactory = handlerFactory;
  }

  @Override
  public void afterPropertiesSet() {
    ApplicationContext context = getApplicationContext();
    Assert.state(context != null, "No ApplicationContext");

    if (handlerFactory == null) {
      handlerFactory = new AnnotationHandlerFactory(context);
      handlerFactory.initDefaults();
    }
    initExceptionHandlerAdviceCache(context);
  }

  private void initExceptionHandlerAdviceCache(ApplicationContext applicationContext) {
    var adviceBeans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);
    for (ControllerAdviceBean adviceBean : adviceBeans) {
      Class<?> beanType = adviceBean.getBeanType();
      if (beanType == null) {
        throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
      }
      var resolver = new ExceptionHandlerMethodResolver(beanType);
      if (resolver.hasExceptionMappings()) {
        exceptionHandlerAdviceCache.put(adviceBean, resolver);
      }
    }

    if (logger.isDebugEnabled()) {
      int handlerSize = exceptionHandlerAdviceCache.size();
      if (handlerSize == 0) {
        logger.debug("ControllerAdvice beans: none");
      }
      else {
        logger.debug("ControllerAdvice beans: {} @ExceptionHandler", handlerSize);
      }
    }
  }

}
