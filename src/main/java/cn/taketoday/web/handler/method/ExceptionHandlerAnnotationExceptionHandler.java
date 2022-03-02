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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.AbstractActionMappingMethodExceptionHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.method.ExceptionHandlerMethodResolver.ExceptionHandlerMappingHandler;

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

  private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerAnnotationExceptionHandler.class);

  private final HashMap<Class<? extends Throwable>, ExceptionHandlerMappingHandler>
          exceptionHandlers = new HashMap<>();

  private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
          new ConcurrentHashMap<>(64);

  private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
          new LinkedHashMap<>();

  /** @since 3.0 */
  private boolean inheritable;

  /** @since 3.0 */
  private ExceptionHandlerMappingHandler globalHandler;

  @Nullable
  private ApplicationContext applicationContext;

  @Nullable
  @Override
  protected Object handleInternal(RequestContext context,
          @Nullable ActionMappingAnnotationHandler annotationHandler, Throwable target) {
    // catch all handlers
    ExceptionHandlerMappingHandler exHandler = lookupExceptionHandler(target);
    if (exHandler == null) {
      return null; // next
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
      ExceptionHandlerMethodResolver resolver = exceptionHandlerCache.computeIfAbsent(
              handlerType, ExceptionHandlerMethodResolver::new);
      ActionMappingAnnotationHandler handler = resolver.lookupHandler(exception);
      if (handler != null) {
        return handler;
      }
      // For advice applicability check below (involving base packages, assignable types
      // and annotation presence), use target class instead of interface-based proxy.
      if (Proxy.isProxyClass(handlerType)) {
        handlerType = AopUtils.getTargetClass(annotationHandler.getHandlerObject());
      }
    }

    for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : exceptionHandlerAdviceCache.entrySet()) {
      ControllerAdviceBean advice = entry.getKey();
      if (advice.isApplicableToBeanType(handlerType)) {
        ExceptionHandlerMethodResolver resolver = entry.getValue();
        ActionMappingAnnotationHandler handler = resolver.lookupHandler(exception);
        if (handler != null) {
          return handler;
        }
      }
    }

    return null;
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

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Nullable
  public ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  @Override
  public void afterPropertiesSet() {
    ApplicationContext context = getApplicationContext();
    if (context != null) {
      initExceptionHandlerAdviceCache(context);
    }
  }

  private void initExceptionHandlerAdviceCache(ApplicationContext applicationContext) {
    List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);
    for (ControllerAdviceBean adviceBean : adviceBeans) {
      Class<?> beanType = adviceBean.getBeanType();
      if (beanType == null) {
        throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
      }
      ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
      if (resolver.hasExceptionMappings()) {
        this.exceptionHandlerAdviceCache.put(adviceBean, resolver);
      }
      if (ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
        this.responseBodyAdvice.add(adviceBean);
      }
    }

    if (logger.isDebugEnabled()) {
      int handlerSize = this.exceptionHandlerAdviceCache.size();
      int adviceSize = this.responseBodyAdvice.size();
      if (handlerSize == 0 && adviceSize == 0) {
        logger.debug("ControllerAdvice beans: none");
      }
      else {
        logger.debug("ControllerAdvice beans: " +
                handlerSize + " @ExceptionHandler, " + adviceSize + " ResponseBodyAdvice");
      }
    }
  }

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
                    handlerBean, parameterFactory, method, manager, errorHandlerType);

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
          Method method, ReturnValueHandlerManager manager, Class<?> errorHandlerType) {
    HandlerMethod handlerMethod = HandlerMethod.from(method);
    ExceptionHandlerMappingHandler handler = new ExceptionHandlerMappingHandler(
            handlerBean, handlerMethod, parameterFactory.createArray(method), errorHandlerType);
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

}
