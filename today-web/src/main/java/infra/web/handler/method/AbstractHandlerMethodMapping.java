/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.handler.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import infra.aop.scope.ScopedProxyUtils;
import infra.aop.support.AopUtils;
import infra.beans.BeanUtils;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.core.MethodIntrospector;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.MapCache;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;
import infra.web.HandlerInterceptor;
import infra.web.HandlerMapping;
import infra.web.InfraConfigurationException;
import infra.web.RequestContext;
import infra.web.annotation.Interceptor;
import infra.web.cors.CorsConfiguration;
import infra.web.handler.AbstractHandlerMapping;
import infra.web.handler.HandlerMethodMappingNamingStrategy;

/**
 * Abstract base class for {@link HandlerMapping} implementations that define
 * a mapping between a request and a {@link HandlerMethod}.
 *
 * <p>For each registered handler method, a unique mapping is maintained with
 * subclasses defining the details of the mapping type {@code <T>}.
 *
 * @param <T> the mapping for a {@link HandlerMethod} containing the conditions
 * needed to match the handler method to an incoming request.
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/1 22:22
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

  /**
   * Bean name prefix for target beans behind scoped proxies. Used to exclude those
   * targets from handler method detection, in favor of the corresponding proxies.
   * <p>We're not checking the autowire-candidate status here, which is how the
   * proxy target filtering problem is being handled at the autowiring level,
   * since autowire-candidate may have been turned to {@code false} for other
   * reasons, while still expecting the bean to be eligible for handler methods.
   * <p>Originally defined in {@link ScopedProxyUtils}
   * but duplicated here to avoid a hard dependency on the aop module.
   */
  private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

  private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH = new HandlerMethod(
          new EmptyHandler(), ReflectionUtils.getMethod(EmptyHandler.class, "handle"));

  private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

  static {
    ALLOW_CORS_CONFIG.addAllowedOriginPattern("*");
    ALLOW_CORS_CONFIG.addAllowedMethod("*");
    ALLOW_CORS_CONFIG.addAllowedHeader("*");
    ALLOW_CORS_CONFIG.setAllowCredentials(true);
  }

  private boolean detectHandlerMethodsInAncestorContexts = false;

  @Nullable
  private HandlerMethodMappingNamingStrategy<T> namingStrategy;

  @Nullable
  private BeanDefinitionRegistry registry;

  @Nullable
  private AnnotatedBeanDefinitionReader beanDefinitionReader;

  private boolean useInheritedInterceptor = true;

  /**
   * Provided for testing purposes.
   */
  final MappingRegistry mappingRegistry = new MappingRegistry();

  /**
   * Whether to detect handler methods in beans in ancestor ApplicationContexts.
   * <p>Default is "false": Only beans in the current ApplicationContext are
   * considered, i.e. only in the context that this HandlerMapping itself
   * is defined in (typically the current DispatcherHandler's context).
   * <p>Switch this flag on to detect handler beans in ancestor contexts
   * (typically the root WebApplicationContext) as well.
   *
   * @see #getCandidateBeanNames()
   */
  public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
    this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
  }

  /**
   * Configure the naming strategy to use for assigning a default name to every
   * mapped handler method.
   * <p>The default naming strategy is based on the capital letters of the
   * class name followed by "#" and then the method name, e.g. "TC#getFoo"
   * for a class named TestController with method getFoo.
   */
  public void setHandlerMethodMappingNamingStrategy(HandlerMethodMappingNamingStrategy<T> namingStrategy) {
    this.namingStrategy = namingStrategy;
  }

  /**
   * Configure the {@code HandlerInterceptor} lookup strategy
   *
   * @see BeanDefinitionRegistry#containsBeanDefinition(Class, boolean)
   * @see #addInterceptors(ApplicationContext, ArrayList, MergedAnnotation)
   */
  public void setUseInheritedInterceptor(boolean useInheritedInterceptor) {
    this.useInheritedInterceptor = useInheritedInterceptor;
  }

  /**
   * Return the configured naming strategy or {@code null}.
   */
  @Nullable
  public HandlerMethodMappingNamingStrategy<T> getNamingStrategy() {
    return this.namingStrategy;
  }

  /**
   * Return a (read-only) map with all mappings and HandlerMethod's.
   */
  public Map<T, HandlerMethod> getHandlerMethods() {
    return Collections.unmodifiableMap(mappingRegistry.registrations.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().handlerMethod)));
  }

  /**
   * Return the handler methods for the given mapping name.
   *
   * @param mappingName the mapping name
   * @return a list of matching HandlerMethod's or {@code null}; the returned
   * list will never be modified and is safe to iterate.
   * @see #setHandlerMethodMappingNamingStrategy
   */
  @Nullable
  public List<HandlerMethod> getHandlerMethodsForMappingName(String mappingName) {
    return this.mappingRegistry.getHandlerMethodsByMappingName(mappingName);
  }

  /**
   * Register the given mapping.
   * <p>This method may be invoked at runtime after initialization has completed.
   *
   * @param mapping the mapping for the handler method
   * @param handler the handler
   * @param method the method
   */
  public void registerMapping(T mapping, Object handler, Method method) {
    if (logger.isTraceEnabled()) {
      logger.trace("Register \"{}\" to {}", mapping, method.toGenericString());
    }
    mappingRegistry.register(mapping, handler, method);
  }

  /**
   * Un-register the given mapping.
   * <p>This method may be invoked at runtime after initialization has completed.
   *
   * @param mapping the mapping to unregister
   */
  public void unregisterMapping(T mapping) {
    if (logger.isTraceEnabled()) {
      logger.trace("Unregister mapping \"{}\"", mapping);
    }
    mappingRegistry.unregister(mapping);
  }

  // Handler method detection

  /**
   * Detects handler methods at initialization.
   *
   * @see #initHandlerMethods
   */
  @Override
  public void afterPropertiesSet() {
    initHandlerMethods();
  }

  /**
   * Scan beans in the ApplicationContext, detect and register handler methods.
   *
   * @see #getCandidateBeanNames()
   * @see #processCandidateBean
   * @see #handlerMethodsInitialized
   */
  protected void initHandlerMethods() {
    BeanFactory beanFactory = obtainApplicationContext().getBeanFactory();
    for (String beanName : getCandidateBeanNames()) {
      if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
        processCandidateBean(beanFactory, beanName);
      }
    }
    handlerMethodsInitialized(getHandlerMethods());
  }

  /**
   * Determine the names of candidate beans in the application context.
   *
   * @see #setDetectHandlerMethodsInAncestorContexts
   * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors
   */
  protected String[] getCandidateBeanNames() {
    return detectHandlerMethodsInAncestorContexts ?
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
            obtainApplicationContext().getBeanNamesForType(Object.class);
  }

  /**
   * Determine the type of the specified candidate bean and call
   * {@link #detectHandlerMethods} if identified as a handler type.
   * <p>This implementation avoids bean creation through checking
   * {@link BeanFactory#getType}
   * and calling {@link #detectHandlerMethods} with the bean name.
   *
   * @param beanName the name of the candidate bean
   * @see #isHandler
   * @see #detectHandlerMethods
   */
  protected void processCandidateBean(BeanFactory beanFactory, String beanName) {
    Class<?> beanType = null;
    try {
      beanType = beanFactory.getType(beanName);
    }
    catch (Throwable ex) {
      // An unresolvable bean type, probably from a lazy bean - let's ignore it.
      logger.trace("Could not resolve type for bean '{}'", beanName, ex);
    }
    if (beanType != null && isHandler(beanType)) {
      detectHandlerMethods(beanType, beanName);
    }
  }

  /**
   * Look for handler methods in the specified handler bean.
   *
   * @param handler either a bean name or an actual handler instance
   * @see #getMappingForMethod
   */
  protected void detectHandlerMethods(Object handler) {
    Class<?> handlerType = handler instanceof String beanName
            ? obtainApplicationContext().getType(beanName)
            : handler.getClass();

    if (handlerType != null) {
      detectHandlerMethods(handlerType, handler);
    }
  }

  /**
   * Look for handler methods in the specified handler bean.
   *
   * @param handlerType handler type
   * @param handler either a bean name or an actual handler instance
   * @see #getMappingForMethod
   */
  private void detectHandlerMethods(Class<?> handlerType, Object handler) {
    Class<?> userType = ClassUtils.getUserClass(handlerType);
    Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
            method -> {
              try {
                return getMappingForMethod(method, userType);
              }
              catch (Throwable ex) {
                throw new IllegalStateException(
                        "Invalid mapping on handler class [%s]: %s".formatted(userType.getName(), method), ex);
              }
            });
    if (logger.isTraceEnabled()) {
      logger.trace(formatMappings(userType, methods));
    }
    else if (mappingsLogger.isDebugEnabled()) {
      mappingsLogger.debug(formatMappings(userType, methods));
    }

    for (Map.Entry<Method, T> entry : methods.entrySet()) {
      Method invocableMethod = AopUtils.selectInvocableMethod(entry.getKey(), userType);
      registerHandlerMethod(handler, invocableMethod, entry.getValue());
    }
  }

  private String formatMappings(Class<?> userType, Map<Method, T> methods) {
    return methods.entrySet().stream()
            .map(e -> {
              Method method = e.getKey();
              return e.getValue() + ": " + method.getName() + Arrays.stream(method.getParameterTypes())
                      .map(Class::getSimpleName)
                      .collect(Collectors.joining(",", "(", ")"));
            })
            .collect(Collectors.joining("\n\t",
                    "\n\t%s:\n\t".formatted(userType.getName()), ""));
  }

  /**
   * Register a handler method and its unique mapping. Invoked at startup for
   * each detected handler method.
   *
   * @param handler the bean name of the handler or the handler instance
   * @param method the method to register
   * @param mapping the mapping conditions associated with the handler method
   * @throws IllegalStateException if another method was already registered
   * under the same mapping
   */
  protected HandlerMethod registerHandlerMethod(Object handler, Method method, T mapping) {
    return mappingRegistry.register(mapping, handler, method);
  }

  /**
   * Create the HandlerMethod instance.
   *
   * @param handler either a bean name or an actual handler instance
   * @param method the target method
   * @return the created HandlerMethod
   */
  protected HandlerMethod createHandlerMethod(Object handler, Method method) {
    if (handler instanceof String beanName) {
      ApplicationContext context = obtainApplicationContext();
      return new HandlerMethod(beanName, context.getBeanFactory(), context, method);
    }
    return new HandlerMethod(handler, method);
  }

  /**
   * Extract and return the CORS configuration for the mapping.
   */
  @Nullable
  protected CorsConfiguration initCorsConfiguration(Object handler, HandlerMethod handlerMethod, Method method, T mapping) {
    return null;
  }

  /**
   * Invoked after all handler methods have been detected.
   *
   * @param handlerMethods a read-only map with handler methods and mappings.
   */
  protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
    // Total includes detected mappings + explicit registrations via registerMapping
    int total = handlerMethods.size();
    if ((logger.isTraceEnabled() && total == 0) || (logger.isDebugEnabled() && total > 0)) {
      logger.debug("{} mappings in {}", total, formatMappingName());
    }
  }

  // Handler method lookup

  /**
   * Look up a handler method for the given request.
   */
  @Nullable
  @Override
  protected HandlerMethod getHandlerInternal(RequestContext request) {
    HandlerMethod handlerMethod = lookupHandlerMethod(request.getRequestPath().value(), request);
    if (handlerMethod != null) {
      Object handler = handlerMethod.getBean();
      if (handler instanceof String beanName) {
        handler = obtainApplicationContext().getBean(beanName);
        return handlerMethod.withBean(handler);
      }
    }
    return handlerMethod;
  }

  /**
   * Look up the best-matching handler method for the current request.
   * If multiple matches are found, the best match is selected.
   *
   * @param directLookupPath mapping lookup path within the current mapping
   * @param request the current request
   * @return the best-matching handler method, or {@code null} if no match
   * @see #handleMatch(Match, String, RequestContext)
   * @see #handleNoMatch(Set, String, RequestContext)
   */
  @Nullable
  protected HandlerMethod lookupHandlerMethod(String directLookupPath, RequestContext request) {
    ArrayList<Match<T>> matches = new ArrayList<>();
    List<T> directPathMatches = mappingRegistry.getDirectPathMappings(directLookupPath);
    if (directPathMatches != null) {
      addMatchingMappings(directPathMatches, matches, request);
    }
    if (matches.isEmpty()) {
      addMatchingMappings(mappingRegistry.registrations.keySet(), matches, request);
    }
    if (matches.isEmpty()) {
      return handleNoMatch(mappingRegistry.registrations.keySet(), directLookupPath, request);
    }
    else {
      Match<T> bestMatch;
      if (matches.size() > 1) {
        var comparator = new MatchComparator(getMappingComparator(request));
        matches.sort(comparator);
        bestMatch = matches.get(0);
        if (logger.isTraceEnabled()) {
          logger.trace("{} matching mappings: {}", matches.size(), matches);
        }
        if (request.isPreFlightRequest()) {
          for (Match<T> match : matches) {
            if (match.hasCorsConfig()) {
              return PREFLIGHT_AMBIGUOUS_MATCH;
            }
          }
        }
        else {
          Match<T> secondBestMatch = matches.get(1);
          if (comparator.compare(bestMatch, secondBestMatch) == 0) {
            Method m1 = bestMatch.getHandlerMethod().getMethod();
            Method m2 = secondBestMatch.getHandlerMethod().getMethod();
            String uri = request.getRequestURI();
            throw new IllegalStateException(
                    "Ambiguous handler methods mapped for '%s': {%s, %s}".formatted(uri, m1, m2));
          }
        }
      }
      else {
        bestMatch = matches.get(0);
      }
      handleMatch(bestMatch, directLookupPath, request);
      return bestMatch.getHandlerMethod();
    }
  }

  private void addMatchingMappings(Collection<T> mappings, ArrayList<Match<T>> matches, RequestContext request) {
    var registrations = mappingRegistry.registrations;
    for (T mapping : mappings) {
      T match = getMatchingMapping(mapping, request);
      if (match != null) {
        matches.add(new Match<>(match, registrations.get(mapping)));
      }
    }
  }

  /**
   * Invoked when a matching mapping is found.
   *
   * @param bestMatch the best match metadata
   * @param directLookupPath mapping lookup path within the current mapping
   * @param request the current request
   */
  protected abstract void handleMatch(Match<T> bestMatch, String directLookupPath, RequestContext request);

  /**
   * Invoked when no matching mapping is not found.
   *
   * @param mappings all registered mappings
   * @param lookupPath mapping lookup path within the current mapping
   * @param request the current request
   */
  @Nullable
  protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, RequestContext request) {

    return null;
  }

  @Override
  protected boolean hasCorsConfigurationSource(Object handler) {
    HandlerMethod handlerMethod;
    return super.hasCorsConfigurationSource(handler)
            || (((handlerMethod = HandlerMethod.unwrap(handler)) != null)
            && mappingRegistry.getCorsConfiguration(handlerMethod) != null);
  }

  @Nullable
  @Override
  protected CorsConfiguration getCorsConfiguration(Object handler, RequestContext request) {
    CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
        return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
      }
      else {
        CorsConfiguration corsConfigFromMethod = mappingRegistry.getCorsConfiguration(handlerMethod);
        if (corsConfig != null) {
          corsConfig = corsConfig.combine(corsConfigFromMethod);
        }
        else {
          corsConfig = corsConfigFromMethod;
        }
      }
    }
    return corsConfig;
  }

  @Nullable
  @Override
  protected HandlerInterceptor[] getHandlerInterceptors(Object handler) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      return mappingRegistry.getHandlerInterceptors(handlerMethod);
    }
    return null;
  }

  /**
   * Get list of intercepters.
   *
   * @param controllerClass controller class
   * @param action method
   * @return List of {@link HandlerInterceptor} objects
   */
  protected List<HandlerInterceptor> getInterceptors(Class<?> controllerClass, Method action) {
    ArrayList<HandlerInterceptor> ret = new ArrayList<>();
    ApplicationContext context = getApplicationContext();

    // get interceptor on class
    MergedAnnotations.from(controllerClass, SearchStrategy.TYPE_HIERARCHY)
            .stream(Interceptor.class)
            .forEach(interceptor -> addInterceptors(context, ret, interceptor));

    // HandlerInterceptor on a method
    MergedAnnotations.from(action, SearchStrategy.TYPE_HIERARCHY)
            .stream(Interceptor.class)
            .forEach(interceptor -> {
              addInterceptors(context, ret, interceptor);

              if (context != null) {
                // exclude interceptors
                for (var exclude : interceptor.<HandlerInterceptor>getClassArray("exclude")) {
                  ret.remove(context.getBean(exclude));
                }

                for (String excludeName : interceptor.getStringArray("excludeNames")) {
                  ret.remove(context.getBean(excludeName, HandlerInterceptor.class));
                }
              }
            });

    return ret;
  }

  private void addInterceptors(@Nullable ApplicationContext context,
          ArrayList<HandlerInterceptor> ret, MergedAnnotation<Interceptor> annotation) {
    var interceptors = annotation.<HandlerInterceptor>getClassValueArray();
    if (context == null) {
      if (ObjectUtils.isNotEmpty(interceptors)) {
        for (var interceptor : interceptors) {
          HandlerInterceptor instance = BeanUtils.newInstance(interceptor);
          ret.add(instance);
        }
      }
      return;
    }

    if (ObjectUtils.isNotEmpty(interceptors)) {
      for (var interceptor : interceptors) {
        if (useInheritedInterceptor) {
          HandlerInterceptor instance = BeanFactoryUtils.find(context, interceptor);
          if (instance == null) {
            registerInterceptorBean(interceptor);
            instance = context.getBean(interceptor);
          }
          ret.add(instance);
        }
        else {
          if (!registry().containsBeanDefinition(interceptor, true)) {
            registerInterceptorBean(interceptor);
          }
          HandlerInterceptor instance = context.getBean(interceptor);
          ret.add(instance);
        }
      }
    }

    // include bean names

    for (String includeName : annotation.getStringArray("includeNames")) {
      Object bean = context.getBean(includeName);
      if (bean instanceof HandlerInterceptor handlerInterceptor) {
        ret.add(handlerInterceptor);
      }
      else {
        throw new IllegalStateException("The bean '%s' is not a HandlerInterceptor".formatted(includeName));
      }
    }
  }

  private void registerInterceptorBean(Class<?> interceptor) {
    if (beanDefinitionReader == null) {
      beanDefinitionReader = new AnnotatedBeanDefinitionReader(registry());
    }
    try {
      beanDefinitionReader.registerBean(interceptor);
    }
    catch (BeanDefinitionStoreException e) {
      throw new InfraConfigurationException(
              "Interceptor: [%s] register error".formatted(interceptor.getName()), e);
    }
  }

  private BeanDefinitionRegistry registry() {
    if (registry == null) {
      registry = unwrapContext(BeanDefinitionRegistry.class);
    }
    return registry;
  }

  // Abstract template methods

  /**
   * Whether the given type is a handler with handler methods.
   *
   * @param beanType the type of the bean being checked
   * @return "true" if this a handler type, "false" otherwise.
   */
  protected abstract boolean isHandler(Class<?> beanType);

  /**
   * Provide the mapping for a handler method. A method for which no
   * mapping can be provided is not a handler method.
   *
   * @param method the method to provide a mapping for
   * @param handlerType the handler type, possibly a sub-type of the method's
   * declaring class
   * @return the mapping, or {@code null} if the method is not mapped
   */
  @Nullable
  protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

  /**
   * Return the request mapping paths that are not patterns.
   */
  protected abstract Set<String> getDirectPaths(T mapping);

  /**
   * Check if a mapping matches the current request and return a (potentially
   * new) mapping with conditions relevant to the current request.
   *
   * @param mapping the mapping to get a match for
   * @param request the current HTTP request
   * @return the match, or {@code null} if the mapping doesn't match
   */
  @Nullable
  protected abstract T getMatchingMapping(T mapping, RequestContext request);

  /**
   * Return a comparator for sorting matching mappings.
   * The returned comparator should sort 'better' matches higher.
   *
   * @param request the current request
   * @return the comparator (never {@code null})
   */
  protected abstract Comparator<T> getMappingComparator(RequestContext request);

  /**
   * A registry that maintains all mappings to handler methods, exposing methods
   * to perform lookups and providing concurrent access.
   * <p>Package-private for testing purposes.
   */
  final class MappingRegistry extends MapCache<Method, HandlerInterceptor[], HandlerMethod> {

    public final ConcurrentHashMap<T, MappingRegistration<T>> registrations = new ConcurrentHashMap<>(128);

    public final ConcurrentHashMap<String, List<T>> pathLookup = new ConcurrentHashMap<>(128);

    public final ConcurrentHashMap<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();

    /**
     * Return matches for the given URL path. Not thread-safe.
     */
    @Nullable
    public List<T> getDirectPathMappings(String urlPath) {
      return pathLookup.get(urlPath);
    }

    /**
     * Return handler methods by mapping name. Thread-safe for concurrent use.
     */
    public List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
      return this.nameLookup.get(mappingName);
    }

    /**
     * Return CORS configuration.
     */
    @Nullable
    public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
      return handlerMethod.corsConfig;
    }

    public HandlerMethod register(T mapping, Object handler, Method method) {
      HandlerMethod handlerMethod = createHandlerMethod(handler, method);
      validateMethodMapping(handlerMethod, mapping);

      Set<String> directPaths = getDirectPaths(mapping);
      for (String path : directPaths) {
        List<T> mappings = pathLookup.get(path);
        if (mappings == null) {
          mappings = new ArrayList<>(1);
          pathLookup.put(path, mappings);
        }
        mappings.add(mapping);
      }

      String name = null;
      var namingStrategy = getNamingStrategy();
      if (namingStrategy != null) {
        name = namingStrategy.getName(handlerMethod, mapping);
        addMappingName(name, handlerMethod);
      }

      CorsConfiguration corsConfig = initCorsConfiguration(handler, handlerMethod, method, mapping);
      if (corsConfig != null) {
        corsConfig.validateAllowCredentials();
        corsConfig.validateAllowPrivateNetwork();
        handlerMethod.corsConfig = corsConfig;
      }

      registrations.put(mapping, new MappingRegistration<>(
              mapping, handlerMethod, directPaths, name, corsConfig != null));

      return handlerMethod;
    }

    private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
      MappingRegistration<T> registration = registrations.get(mapping);
      HandlerMethod existingHandlerMethod = registration != null ? registration.handlerMethod : null;
      if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
        throw new IllegalStateException(
                "Ambiguous mapping. Cannot map '%s' method \n%s\nto %s: There is already '%s' bean method\n%s mapped."
                        .formatted(handlerMethod.getBean(), handlerMethod, mapping, existingHandlerMethod.getBean(), existingHandlerMethod));
      }
    }

    private void addMappingName(String name, HandlerMethod handlerMethod) {
      List<HandlerMethod> oldList = nameLookup.get(name);
      if (oldList == null) {
        oldList = Collections.emptyList();
      }

      for (HandlerMethod current : oldList) {
        if (handlerMethod.equals(current)) {
          return;
        }
      }

      ArrayList<HandlerMethod> newList = new ArrayList<>(oldList.size() + 1);
      newList.addAll(oldList);
      newList.add(handlerMethod);
      nameLookup.put(name, newList);
    }

    public void unregister(T mapping) {
      MappingRegistration<T> registration = registrations.remove(mapping);
      if (registration == null) {
        return;
      }

      for (String path : registration.directPaths) {
        List<T> mappings = pathLookup.get(path);
        if (mappings != null) {
          mappings.remove(registration.mapping);
          if (mappings.isEmpty()) {
            pathLookup.remove(path);
          }
        }
      }

      removeMappingName(registration);
    }

    private void removeMappingName(MappingRegistration<T> definition) {
      String name = definition.mappingName;
      if (name == null) {
        return;
      }
      HandlerMethod handlerMethod = definition.handlerMethod;
      List<HandlerMethod> oldList = nameLookup.get(name);
      if (oldList == null) {
        return;
      }
      if (oldList.size() <= 1) {
        nameLookup.remove(name);
        return;
      }
      ArrayList<HandlerMethod> newList = new ArrayList<>(oldList.size() - 1);
      for (HandlerMethod current : oldList) {
        if (!current.equals(handlerMethod)) {
          newList.add(current);
        }
      }
      nameLookup.put(name, newList);
    }

    @Override
    protected HandlerInterceptor[] createValue(Method method, HandlerMethod handlerMethod) {
      Class<?> controllerClass = getControllerClass(method, handlerMethod);
      List<HandlerInterceptor> interceptors = getInterceptors(controllerClass, method);
      return interceptors.toArray(HandlerInterceptor.EMPTY_ARRAY);
    }

    private Class<?> getControllerClass(Method method, @Nullable HandlerMethod handlerMethod) {
      if (handlerMethod != null) {
        return handlerMethod.getBeanType();
      }
      return ClassUtils.getUserClass(method.getDeclaringClass());
    }

    public HandlerInterceptor[] getHandlerInterceptors(HandlerMethod handlerMethod) {
      Method method = handlerMethod.getMethod();
      return get(method, handlerMethod);
    }
  }

  static class MappingRegistration<T> {
    public final T mapping;

    @Nullable
    public final String mappingName;

    public final boolean hasCorsConfig;

    public final Set<String> directPaths;

    public final HandlerMethod handlerMethod;

    MappingRegistration(T mapping, HandlerMethod handlerMethod,
            @Nullable Set<String> directPaths, @Nullable String mappingName, boolean hasCorsConfig) {
      Assert.notNull(mapping, "Mapping is required");
      Assert.notNull(handlerMethod, "HandlerMethod is required");
      this.mapping = mapping;
      this.mappingName = mappingName;
      this.hasCorsConfig = hasCorsConfig;
      this.handlerMethod = handlerMethod;
      this.directPaths = directPaths != null ? directPaths : Collections.emptySet();
    }
  }

  /**
   * A thin wrapper around a matched HandlerMethod and its mapping, for the purpose of
   * comparing the best match with a comparator in the context of the current request.
   *
   * @param <T> mapping type
   */
  protected static final class Match<T> {

    public final T mapping;

    public final MappingRegistration<T> registration;

    public Match(T mapping, MappingRegistration<T> registration) {
      this.mapping = mapping;
      this.registration = registration;
    }

    public HandlerMethod getHandlerMethod() {
      return this.registration.handlerMethod;
    }

    public boolean hasCorsConfig() {
      return this.registration.hasCorsConfig;
    }

    @Override
    public String toString() {
      return this.mapping.toString();
    }
  }

  private class MatchComparator implements Comparator<Match<T>> {

    private final Comparator<T> comparator;

    public MatchComparator(Comparator<T> comparator) {
      this.comparator = comparator;
    }

    @Override
    public int compare(Match<T> match1, Match<T> match2) {
      return this.comparator.compare(match1.mapping, match2.mapping);
    }
  }

  private static final class EmptyHandler {

    @SuppressWarnings("unused")
    public void handle() {
      throw new UnsupportedOperationException("Not implemented");
    }
  }

}
