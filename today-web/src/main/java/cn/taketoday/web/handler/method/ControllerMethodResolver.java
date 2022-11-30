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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.bind.annotation.InitBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.support.SessionAttributeStore;
import cn.taketoday.web.handler.ReturnValueHandlerManager;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/30 22:33
 */
class ControllerMethodResolver {
  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * MethodFilter that matches {@link InitBinder @InitBinder} methods.
   */
  public static final ReflectionUtils.MethodFilter INIT_BINDER_METHODS = method ->
          AnnotatedElementUtils.hasAnnotation(method, InitBinder.class);

  /**
   * MethodFilter that matches {@link ModelAttribute @ModelAttribute} methods.
   */
  public static final ReflectionUtils.MethodFilter MODEL_ATTRIBUTE_METHODS = method ->
          !AnnotatedElementUtils.hasAnnotation(method, ActionMapping.class)
                  && AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class);

  private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache = new ConcurrentHashMap<>(64);

  private final Map<Class<?>, Set<Method>> initBinderCache = new ConcurrentHashMap<>(64);

  private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>();
  private final Map<Class<?>, Set<Method>> modelAttributeCache = new ConcurrentHashMap<>(64);
  private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>();
  private final Map<HandlerMethod, ResultableHandlerMethod> invocableHandlerMethodMap = new ConcurrentHashMap<>();

  private final SessionAttributeStore sessionAttributeStore;
  private final ResolvableParameterFactory resolvableParameterFactory;
  private final ReturnValueHandlerManager returnValueHandlerManager;

  ControllerMethodResolver(ApplicationContext context,
          SessionAttributeStore sessionAttributeStore,
          ResolvableParameterFactory resolvableParameterFactory,
          ReturnValueHandlerManager returnValueHandlerManager) {
    this.sessionAttributeStore = sessionAttributeStore;
    this.resolvableParameterFactory = resolvableParameterFactory;
    this.returnValueHandlerManager = returnValueHandlerManager;
    initControllerAdviceCache(context);
  }

  private void initControllerAdviceCache(ApplicationContext context) {
    var adviceBeans = ControllerAdviceBean.findAnnotatedBeans(context);
    for (ControllerAdviceBean adviceBean : adviceBeans) {
      Class<?> beanType = adviceBean.getBeanType();
      if (beanType == null) {
        throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
      }
      Set<Method> attrMethods = MethodIntrospector.filterMethods(beanType, MODEL_ATTRIBUTE_METHODS);
      if (!attrMethods.isEmpty()) {
        this.modelAttributeAdviceCache.put(adviceBean, attrMethods);
      }
      Set<Method> binderMethods = MethodIntrospector.filterMethods(beanType, INIT_BINDER_METHODS);
      if (!binderMethods.isEmpty()) {
        this.initBinderAdviceCache.put(adviceBean, binderMethods);
      }
    }

    if (log.isDebugEnabled()) {
      int binderSize = this.initBinderAdviceCache.size();
      int modelSize = this.modelAttributeAdviceCache.size();
      if (modelSize == 0 && binderSize == 0) {
        log.debug("ControllerAdvice beans: none");
      }
      else {
        log.debug("ControllerAdvice beans: {} @ModelAttribute, {} @InitBinder", modelSize, binderSize);
      }
    }
  }

  /**
   * Return the {@link SessionAttributesHandler} instance for the given handler type
   * (never {@code null}).
   */
  public SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
    return this.sessionAttributesHandlerCache.computeIfAbsent(
            handlerMethod.getBeanType(),
            type -> new SessionAttributesHandler(type, this.sessionAttributeStore));
  }

  public ModelInitializer getModelInitializer(HandlerMethod handlerMethod) {
    Class<?> handlerType = handlerMethod.getBeanType();

    ArrayList<InvocableHandlerMethod> attrMethods = new ArrayList<>();
    // Global methods first
    for (var entry : modelAttributeAdviceCache.entrySet()) {
      ControllerAdviceBean controllerAdviceBean = entry.getKey();
      if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
        Object bean = controllerAdviceBean.resolveBean();
        for (Method method : entry.getValue()) {
          attrMethods.add(createModelAttributeMethod(bean, method));
        }
      }
    }
    // controller local methods
    Set<Method> methods = modelAttributeCache.get(handlerType);
    if (methods == null) {
      methods = MethodIntrospector.filterMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
      modelAttributeCache.put(handlerType, methods);
    }
    for (Method method : methods) {
      Object bean = handlerMethod.getBean();
      attrMethods.add(createModelAttributeMethod(bean, method));
    }

    SessionAttributesHandler sessionAttrHandler = getSessionAttributesHandler(handlerMethod);
    return new ModelInitializer(attrMethods, sessionAttrHandler);
  }

  public List<InvocableHandlerMethod> getBinderMethods(HandlerMethod handlerMethod) {
    Class<?> handlerType = handlerMethod.getBeanType();
    Set<Method> methods = initBinderCache.get(handlerType);
    if (methods == null) {
      methods = MethodIntrospector.filterMethods(handlerType, INIT_BINDER_METHODS);
      initBinderCache.put(handlerType, methods);
    }

    var initBinderMethods = new ArrayList<InvocableHandlerMethod>();
    // Global methods first
    for (var entry : initBinderAdviceCache.entrySet()) {
      Set<Method> methodSet = entry.getValue();
      ControllerAdviceBean controllerAdviceBean = entry.getKey();
      if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
        Object bean = controllerAdviceBean.resolveBean();
        for (Method method : methodSet) {
          initBinderMethods.add(createInitBinderMethod(bean, method));
        }
      }
    }

    for (Method method : methods) {
      Object bean = handlerMethod.getBean();
      initBinderMethods.add(createInitBinderMethod(bean, method));
    }
    return initBinderMethods;
  }

  /**
   * Create a {@link ResultableHandlerMethod} from the given {@link HandlerMethod} definition.
   *
   * @param handlerMethod the {@link HandlerMethod} definition
   * @return the corresponding {@link ResultableHandlerMethod} (or custom subclass thereof)
   */
  public ResultableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
    return invocableHandlerMethodMap.computeIfAbsent(handlerMethod,
            handler -> new ResultableHandlerMethod(
                    handler, returnValueHandlerManager, resolvableParameterFactory));
  }

  private InvocableHandlerMethod createModelAttributeMethod(Object bean, Method method) {
    return new InvocableHandlerMethod(bean, method, resolvableParameterFactory);
  }

  private InvocableHandlerMethod createInitBinderMethod(Object bean, Method method) {
    return new InvocableHandlerMethod(bean, method, resolvableParameterFactory);
  }

}
