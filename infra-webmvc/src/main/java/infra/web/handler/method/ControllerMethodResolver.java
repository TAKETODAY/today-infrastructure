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
 * Package-private class to assist {@link RequestMappingHandlerAdapter} with
 * resolving, initializing, and caching annotated methods declared in
 * {@code @Controller} and {@code @ControllerAdvice} components.
 *
 * <p>Assists with the following annotations:
 * <ul>
 * <li>{@code @InitBinder}
 * <li>{@code @ModelAttribute}
 * <li>{@code @RequestMapping}
 * <li>{@code @ExceptionHandler}
 * </ul>
 *
 * @author Rossen Stoyanchev
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

  private final HandlerParameterFactory parameterFactory;

  ControllerMethodResolver(@Nullable ApplicationContext context, HandlerParameterFactory parameterFactory) {
    this.parameterFactory = parameterFactory;

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

  public List<InvocableHandlerMethod> getModelAttributeMethods(InvocableHandlerMethod handlerMethod) {
    List<InvocableHandlerMethod> cached = handlerMethod.getCachedModelAttributeMethods();
    if (cached != null) {
      return cached;
    }
    synchronized(handlerMethod) {
      cached = handlerMethod.getCachedModelAttributeMethods();
      if (cached == null) {
        cached = resolveModelAttributeMethods(handlerMethod);
        if (isModelAttributeMethodsCacheable(handlerMethod.getBeanType())) {
          handlerMethod.setCachedModelAttributeMethods(cached);
        }
      }
      return cached;
    }
  }

  private List<InvocableHandlerMethod> resolveModelAttributeMethods(HandlerMethod handlerMethod) {
    Class<?> handlerType = handlerMethod.getBeanType();

    ArrayList<InvocableHandlerMethod> attrMethods = null;
    // Global methods first
    if (!modelAttributeAdviceCache.isEmpty()) {
      for (var entry : modelAttributeAdviceCache.entrySet()) {
        ControllerAdviceBean controllerAdviceBean = entry.getKey();
        if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
          Object bean = controllerAdviceBean.resolveBean();
          if (attrMethods == null) {
            attrMethods = new ArrayList<>();
          }
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
      if (attrMethods == null) {
        attrMethods = new ArrayList<>(methods.size());
      }
      for (Method method : methods) {
        Object bean = handlerMethod.getBean();
        attrMethods.add(createHandlerMethod(bean, method));
      }
    }

    return attrMethods != null ? attrMethods : Collections.emptyList();
  }

  /**
   * @return non-null InvocableHandlerMethods
   */
  public List<InvocableHandlerMethod> getBinderMethods(InvocableHandlerMethod handlerMethod) {
    List<InvocableHandlerMethod> cached = handlerMethod.getCachedBinderMethods();
    if (cached != null) {
      return cached;
    }
    synchronized(handlerMethod) {
      cached = handlerMethod.getCachedBinderMethods();
      if (cached == null) {
        cached = resolveBinderMethods(handlerMethod);
        if (isBinderMethodsCacheable(handlerMethod.getBeanType())) {
          handlerMethod.setCachedBinderMethods(cached);
        }
      }
      return cached;
    }
  }

  private List<InvocableHandlerMethod> resolveBinderMethods(HandlerMethod handlerMethod) {
    Class<?> handlerType = handlerMethod.getBeanType();
    Set<Method> methods = initBinderCache.get(handlerType);
    if (methods == null) {
      methods = MethodIntrospector.filterMethods(handlerType, INIT_BINDER_METHODS);
      initBinderCache.put(handlerType, methods);
    }

    ArrayList<InvocableHandlerMethod> initBinderMethods = null;
    // Global methods first
    for (var entry : initBinderAdviceCache.entrySet()) {
      Set<Method> methodSet = entry.getValue();
      ControllerAdviceBean controllerAdviceBean = entry.getKey();
      if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
        Object bean = controllerAdviceBean.resolveBean();
        if (initBinderMethods == null) {
          initBinderMethods = new ArrayList<>();
        }
        for (Method method : methodSet) {
          initBinderMethods.add(createHandlerMethod(bean, method));
        }
      }
    }

    if (!methods.isEmpty()) {
      if (initBinderMethods == null) {
        initBinderMethods = new ArrayList<>(methods.size());
      }
      for (Method method : methods) {
        Object bean = handlerMethod.getBean();
        initBinderMethods.add(createHandlerMethod(bean, method));
      }
    }
    return initBinderMethods != null ? initBinderMethods : Collections.emptyList();
  }

  private boolean isModelAttributeMethodsCacheable(Class<?> handlerType) {
    return modelAttributeAdviceCache.keySet().stream()
            .filter(adviceBean -> adviceBean.isApplicableToBeanType(handlerType))
            .allMatch(ControllerAdviceBean::isSingleton);
  }

  private boolean isBinderMethodsCacheable(Class<?> handlerType) {
    return initBinderAdviceCache.keySet().stream()
            .filter(adviceBean -> adviceBean.isApplicableToBeanType(handlerType))
            .allMatch(ControllerAdviceBean::isSingleton);
  }

  /**
   * Reuse a mapped {@link InvocableHandlerMethod}, or adapt a plain
   * {@link HandlerMethod} for the current invocation.
   *
   * @param handlerMethod the {@link HandlerMethod} definition
   * @return the corresponding {@link InvocableHandlerMethod} (or custom subclass thereof)
   */
  public InvocableHandlerMethod createHandlerMethod(HandlerMethod handlerMethod) {
    if (handlerMethod instanceof InvocableHandlerMethod invocableHandlerMethod) {
      return invocableHandlerMethod;
    }
    return new InvocableHandlerMethod(handlerMethod, parameterFactory);
  }

  private InvocableHandlerMethod createHandlerMethod(Object bean, Method method) {
    return new InvocableHandlerMethod(bean, method, parameterFactory);
  }

}
