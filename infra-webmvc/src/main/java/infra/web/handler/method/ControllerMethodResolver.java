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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import infra.aop.support.AopUtils;
import infra.beans.factory.BeanFactoryUtils;
import infra.context.ApplicationContext;
import infra.context.MessageSource;
import infra.core.MethodIntrospector;
import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.core.annotation.AnnotatedElementUtils;
import infra.http.MediaType;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.Assert;
import infra.util.ReflectionUtils;
import infra.util.function.SingletonSupplier;
import infra.web.HandlerMatchingMetadata;
import infra.web.HttpContext;
import infra.web.annotation.RequestMapping;
import infra.web.bind.annotation.InitBinder;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.handler.method.support.ParameterResolvingRegistryFactory;

/**
 * Assist {@link RequestMappingHandlerAdapter} and
 * {@link ExceptionHandlerAnnotationExceptionHandler} with resolving and caching annotated methods declared in
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
public final class ControllerMethodResolver {

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

  private final Map<Class<?>, Set<Method>> initBinderCache;

  private final Map<Class<?>, Set<Method>> modelAttributeCache;

  private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache;

  private final @Nullable ApplicationContext applicationContext;

  private final SingletonSupplier<List<ControllerAdviceBean>> adviceBeans;

  private final SingletonSupplier<BindingAdvice> bindingAdvice;

  private final SingletonSupplier<Map<ControllerAdviceBean, ExceptionHandlerMethodResolver>> exceptionAdvice;

  private final @Nullable ParameterResolvingRegistry parameterRegistry;

  private final ParameterNameDiscoverer parameterNameDiscoverer;

  private final HandlerParameterFactory parameterFactory;

  /**
   * Create a resolver for one MVC application context without discovering advice yet.
   * A {@code null} context supports standalone handlers without global advice;
   * a {@code null} registry uses the basic parameter factory.
   */
  public ControllerMethodResolver(@Nullable ApplicationContext context, @Nullable ParameterResolvingRegistry registry) {
    this(context, registry, ParameterNameDiscoverer.getSharedInstance());
  }

  /**
   * Create a resolver with the given registry and parameter name discovery strategy.
   */
  public ControllerMethodResolver(@Nullable ApplicationContext context, @Nullable ParameterResolvingRegistry registry,
          ParameterNameDiscoverer discoverer) {
    this.applicationContext = context;
    this.adviceBeans = SingletonSupplier.of(() -> context != null ?
            List.copyOf(ControllerAdviceBean.findAnnotatedBeans(context)) : List.of());
    this.bindingAdvice = SingletonSupplier.of(this::discoverBindingAdvice);
    this.exceptionAdvice = SingletonSupplier.of(this::discoverExceptionAdvice);
    this.initBinderCache = new ConcurrentHashMap<>(64);
    this.modelAttributeCache = new ConcurrentHashMap<>(64);
    this.exceptionHandlerCache = new ConcurrentHashMap<>(64);
    this.parameterRegistry = registry;
    this.parameterNameDiscoverer = discoverer;
    this.parameterFactory = createParameterFactory(registry, discoverer);
  }

  private ControllerMethodResolver(ControllerMethodResolver source, @Nullable ParameterResolvingRegistry registry,
          ParameterNameDiscoverer discoverer) {
    this.applicationContext = source.applicationContext;
    this.adviceBeans = source.adviceBeans;
    this.bindingAdvice = source.bindingAdvice;
    this.exceptionAdvice = source.exceptionAdvice;
    this.initBinderCache = source.initBinderCache;
    this.modelAttributeCache = source.modelAttributeCache;
    this.exceptionHandlerCache = source.exceptionHandlerCache;
    this.parameterRegistry = registry;
    this.parameterNameDiscoverer = discoverer;
    this.parameterFactory = createParameterFactory(registry, discoverer);
  }

  private static HandlerParameterFactory createParameterFactory(@Nullable ParameterResolvingRegistry registry,
          ParameterNameDiscoverer discoverer) {
    // A Method alone does not identify its resolved generic types or its bound target.
    return registry != null ? new RegistryHandlerParameterFactory(registry, discoverer, false)
            : new HandlerParameterFactory(discoverer, false);
  }

  static ControllerMethodResolver forRequestHandlers(@Nullable ControllerMethodResolver shared,
          @Nullable ApplicationContext context) {
    var registry = shared != null ? shared.parameterRegistry : null;
    var discoverer = shared != null ? shared.parameterNameDiscoverer : ParameterNameDiscoverer.getSharedInstance();
    if (registry == null) {
      Assert.state(context != null, "No ApplicationContext");
      registry = ParameterResolvingRegistryFactory.getOrCreate(context);
    }
    if (shared == null || (shared.applicationContext == null && context != null)) {
      return new ControllerMethodResolver(context, registry, discoverer);
    }
    return shared.withParameterConfiguration(registry, discoverer);
  }

  static ControllerMethodResolver forExceptionHandlers(ApplicationContext context) {
    return new ControllerMethodResolver(context, BeanFactoryUtils.find(context, ParameterResolvingRegistry.class));
  }

  /**
   * Return a resolver with different parameter settings that shares this resolver's method metadata.
   */
  public ControllerMethodResolver withParameterConfiguration(@Nullable ParameterResolvingRegistry registry,
          ParameterNameDiscoverer discoverer) {
    if (this.parameterRegistry == registry && this.parameterNameDiscoverer == discoverer) {
      return this;
    }
    return new ControllerMethodResolver(this, registry, discoverer);
  }

  ParameterNameDiscoverer getParameterNameDiscoverer() {
    return this.parameterNameDiscoverer;
  }

  Predicate<MethodParameter> resolvedBy(Class<? extends ParameterResolvingStrategy> strategyType) {
    var registry = this.parameterRegistry;
    return registry != null ? parameter -> strategyType.isInstance(registry.findStrategy(new HandlerParameter(parameter)))
            : parameter -> false;
  }

  void initBindingAdvice() {
    this.bindingAdvice.obtain();
  }

  void initExceptionAdvice() {
    this.exceptionAdvice.obtain();
  }

  private BindingAdvice discoverBindingAdvice() {
    var modelMethods = new LinkedHashMap<ControllerAdviceBean, Set<Method>>();
    var binderMethods = new LinkedHashMap<ControllerAdviceBean, Set<Method>>();
    for (ControllerAdviceBean adviceBean : this.adviceBeans.obtain()) {
      Class<?> beanType = adviceType(adviceBean);
      Set<Method> attrMethods = MethodIntrospector.filterMethods(beanType, MODEL_ATTRIBUTE_METHODS);
      if (!attrMethods.isEmpty()) {
        modelMethods.put(adviceBean, attrMethods);
      }
      Set<Method> initMethods = MethodIntrospector.filterMethods(beanType, INIT_BINDER_METHODS);
      if (!initMethods.isEmpty()) {
        binderMethods.put(adviceBean, initMethods);
      }
    }
    log.debug("ControllerAdvice beans: {} @ModelAttribute, {} @InitBinder", modelMethods.size(), binderMethods.size());
    return new BindingAdvice(binderMethods, modelMethods);
  }

  private Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> discoverExceptionAdvice() {
    var handlers = new LinkedHashMap<ControllerAdviceBean, ExceptionHandlerMethodResolver>();
    for (ControllerAdviceBean adviceBean : this.adviceBeans.obtain()) {
      var resolver = new ExceptionHandlerMethodResolver(adviceType(adviceBean));
      if (resolver.hasExceptionMappings()) {
        handlers.put(adviceBean, resolver);
      }
    }
    log.debug("ControllerAdvice beans: {} @ExceptionHandler", handlers.size());
    return handlers;
  }

  private static Class<?> adviceType(ControllerAdviceBean advice) {
    Class<?> beanType = advice.getBeanType();
    if (beanType == null) {
      throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + advice);
    }
    return beanType;
  }

  List<InvocableHandlerMethod> getModelAttributeMethods(InvocableHandlerMethod handler) {
    return getBindingMethods(handler, false);
  }

  List<InvocableHandlerMethod> getBinderMethods(InvocableHandlerMethod handler) {
    return getBindingMethods(handler, true);
  }

  private List<InvocableHandlerMethod> getBindingMethods(InvocableHandlerMethod handler, boolean binder) {

    var cached = handler.getBindingMethods(binder);
    if (cached == null) {
      synchronized(handler) {
        cached = handler.getBindingMethods(binder);
        if (cached == null) {
          var resolved = resolveBindingMethods(handler, binder);
          if (resolved.cacheable()) {
            handler.setBindingMethods(binder, new InvocableHandlerMethod.BindingMethods(this, resolved.methods()));
          }
          return resolved.methods();
        }
      }
    }
    // A handler can be used by another adapter. Keep only the first configuration in its cache.
    return cached.resolver() == this ? cached.methods() : resolveBindingMethods(handler, binder).methods();
  }

  private ResolvedBindingMethods resolveBindingMethods(HandlerMethod handler, boolean binder) {
    Class<?> handlerType = handler.getBeanType();
    var advice = this.bindingAdvice.obtain();
    var adviceMethods = binder ? advice.binderMethods() : advice.modelMethods();
    var localMethods = binder ? initBinderCache.computeIfAbsent(handlerType,
            type -> MethodIntrospector.filterMethods(type, INIT_BINDER_METHODS)) :
            modelAttributeCache.computeIfAbsent(handlerType, type -> MethodIntrospector.filterMethods(type, MODEL_ATTRIBUTE_METHODS));

    ArrayList<InvocableHandlerMethod> methods = null;
    boolean cacheable = true;
    // Global methods first, in advice order.
    for (var entry : adviceMethods.entrySet()) {
      ControllerAdviceBean adviceBean = entry.getKey();
      if (adviceBean.isApplicableToBeanType(handlerType)) {
        Object bean = adviceBean.resolveBean();
        cacheable &= adviceBean.isSingleton();
        if (methods == null) {
          methods = new ArrayList<>(entry.getValue().size() + localMethods.size());
        }
        for (Method method : entry.getValue()) {
          methods.add(createHandlerMethod(bean, method, null));
        }
      }
    }
    if (!localMethods.isEmpty()) {
      if (methods == null) {
        methods = new ArrayList<>(localMethods.size());
      }
      for (Method method : localMethods) {
        methods.add(createHandlerMethod(handler.getBean(), method, null));
      }
    }
    return new ResolvedBindingMethods(methods != null ? List.copyOf(methods) : List.of(), cacheable);
  }

  boolean hasExceptionHandlerAdvice() {
    return !this.exceptionAdvice.obtain().isEmpty();
  }

  @Nullable
  InvocableHandlerMethod resolveExceptionHandlerMethod(HttpContext context, @Nullable HandlerMethod handlerMethod,
          Throwable exception, List<MediaType> acceptedMediaTypes) {

    Class<?> handlerType = null;
    if (handlerMethod != null) {
      handlerType = handlerMethod.getBeanType();
      var resolver = exceptionHandlerCache.computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new);
      var mapping = resolveExceptionMapping(resolver, exception, acceptedMediaTypes);
      if (mapping != null) {
        return createExceptionHandlerMethod(context, handlerMethod.getBean(), mapping);
      }

      // Match advice selectors against the target class rather than a JDK proxy.
      if (Proxy.isProxyClass(handlerType)) {
        handlerType = AopUtils.getTargetClass(handlerMethod.getBean());
      }
    }

    for (var entry : this.exceptionAdvice.obtain().entrySet()) {
      ControllerAdviceBean advice = entry.getKey();
      if (advice.isApplicableToBeanType(handlerType)) {
        var mapping = resolveExceptionMapping(entry.getValue(), exception, acceptedMediaTypes);
        if (mapping != null) {
          return createExceptionHandlerMethod(context, advice.resolveBean(), mapping);
        }
      }
    }
    return null;
  }

  private static @Nullable ExceptionHandlerMappingInfo resolveExceptionMapping(ExceptionHandlerMethodResolver resolver,
          Throwable exception, List<MediaType> acceptedMediaTypes) {

    for (MediaType mediaType : acceptedMediaTypes) {
      var mapping = resolver.resolveExceptionMapping(exception, mediaType);
      if (mapping != null) {
        return mapping;
      }
    }
    return null;
  }

  private InvocableHandlerMethod createExceptionHandlerMethod(HttpContext context, Object bean,
          ExceptionHandlerMappingInfo mapping) {
    if (!mapping.getProducibleTypes().isEmpty()) {
      var metadata = HandlerMatchingMetadata.find(context);
      if (metadata != null) {
        metadata.setProducibleMediaTypes(mapping.getProducibleTypes());
      }
    }
    return createHandlerMethod(bean, mapping.getHandlerMethod(), this.applicationContext);
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
    return new InvocableHandlerMethod(handlerMethod, this.parameterFactory);
  }

  InvocableHandlerMethod createMappedHandlerMethod(Object bean, Method method, @Nullable ApplicationContext context) {
    if (bean instanceof String beanName) {
      if (context == null) {
        throw new IllegalStateException("ApplicationContext is required for a bean name handler");
      }
      return new InvocableHandlerMethod(beanName, context.getBeanFactory(), context, method, this.parameterFactory);
    }
    return createHandlerMethod(bean, method, null);
  }

  InvocableHandlerMethod createHandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
    return new InvocableHandlerMethod(bean, method, messageSource, this.parameterFactory);
  }

  private record BindingAdvice(Map<ControllerAdviceBean, Set<Method>> binderMethods,
          Map<ControllerAdviceBean, Set<Method>> modelMethods) {
  }

  private record ResolvedBindingMethods(List<InvocableHandlerMethod> methods, boolean cacheable) {
  }

}
