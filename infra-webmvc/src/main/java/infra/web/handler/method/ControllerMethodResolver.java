/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import infra.context.ApplicationContext;
import infra.core.MethodIntrospector;
import infra.core.annotation.AnnotatedElementUtils;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ReflectionUtils;
import infra.web.annotation.RequestMapping;
import infra.web.bind.annotation.InitBinder;
import infra.web.bind.annotation.ModelAttribute;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/30 22:33
 */
final class ControllerMethodResolver {
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
          !AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)
                  && AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class);

  private final Map<Class<?>, Set<Method>> initBinderCache = new ConcurrentHashMap<>(64);

  private final LinkedHashMap<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>();

  private final ConcurrentHashMap<Class<?>, Set<Method>> modelAttributeCache = new ConcurrentHashMap<>(64);

  private final LinkedHashMap<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>();

  private final ConcurrentHashMap<HandlerMethod, InvocableHandlerMethod> invocableHandlerMethodMap = new ConcurrentHashMap<>();

  private final ResolvableParameterFactory resolvableParameterFactory;

  ControllerMethodResolver(@Nullable ApplicationContext context, ResolvableParameterFactory parameterFactory) {
    this.resolvableParameterFactory = parameterFactory;

    if (context != null) {
      initControllerAdviceCache(context);
    }
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

  public List<InvocableHandlerMethod> getModelAttributeMethods(HandlerMethod handlerMethod) {
    Class<?> handlerType = handlerMethod.getBeanType();

    ArrayList<InvocableHandlerMethod> attrMethods = new ArrayList<>();
    // Global methods first
    if (!modelAttributeAdviceCache.isEmpty()) {
      for (var entry : modelAttributeAdviceCache.entrySet()) {
        ControllerAdviceBean controllerAdviceBean = entry.getKey();
        if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
          Object bean = controllerAdviceBean.resolveBean();
          for (Method method : entry.getValue()) {
            attrMethods.add(createHandlerMethod(bean, method));
          }
        }
      }
    }
    // controller local methods
    Set<Method> methods = modelAttributeCache.get(handlerType);
    if (methods == null) {
      methods = MethodIntrospector.filterMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
      modelAttributeCache.put(handlerType, methods);
    }

    if (!methods.isEmpty()) {
      for (Method method : methods) {
        Object bean = handlerMethod.getBean();
        attrMethods.add(createHandlerMethod(bean, method));
      }
    }

    return attrMethods;
  }

  /**
   * @return non-null InvocableHandlerMethods
   */
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
          initBinderMethods.add(createHandlerMethod(bean, method));
        }
      }
    }

    for (Method method : methods) {
      Object bean = handlerMethod.getBean();
      initBinderMethods.add(createHandlerMethod(bean, method));
    }
    return initBinderMethods.isEmpty() ? Collections.emptyList() : initBinderMethods;
  }

  /**
   * Create a {@link InvocableHandlerMethod} from the given {@link HandlerMethod} definition.
   *
   * @param handlerMethod the {@link HandlerMethod} definition
   * @return the corresponding {@link InvocableHandlerMethod} (or custom subclass thereof)
   */
  public InvocableHandlerMethod createHandlerMethod(HandlerMethod handlerMethod) {
    return invocableHandlerMethodMap.computeIfAbsent(handlerMethod,
            handler -> new InvocableHandlerMethod(handler, resolvableParameterFactory));
  }

  private InvocableHandlerMethod createHandlerMethod(Object bean, Method method) {
    return new InvocableHandlerMethod(bean, method, resolvableParameterFactory);
  }

}
