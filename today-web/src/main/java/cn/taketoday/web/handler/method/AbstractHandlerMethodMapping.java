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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.MapCache;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.FrameworkConfigurationException;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.handler.HandlerMethodMappingNamingStrategy;
import cn.taketoday.web.registry.AbstractHandlerMapping;

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
   * <p>Originally defined in {@link cn.taketoday.aop.scope.ScopedProxyUtils}
   * but duplicated here to avoid a hard dependency on the aop module.
   */
  private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

  private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
          new HandlerMethod(new EmptyHandler(), ReflectionUtils.getMethod(EmptyHandler.class, "handle"));

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

  private GenericApplicationContext context;

  private boolean useInheritedInterceptor = true;

  private final MappingRegistry mappingRegistry = new MappingRegistry();

  /**
   * Whether to detect handler methods in beans in ancestor ApplicationContexts.
   * <p>Default is "false": Only beans in the current ApplicationContext are
   * considered, i.e. only in the context that this HandlerMapping itself
   * is defined in (typically the current DispatcherServlet's context).
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
    this.mappingRegistry.acquireReadLock();
    try {
      return Collections.unmodifiableMap(
              this.mappingRegistry.getRegistrations().entrySet().stream()
                      .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().handlerMethod)));
    }
    finally {
      this.mappingRegistry.releaseReadLock();
    }
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
   * Return the internal mapping registry. Provided for testing purposes.
   */
  MappingRegistry getMappingRegistry() {
    return this.mappingRegistry;
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
    if (log.isTraceEnabled()) {
      log.trace("Register \"{}\" to {}", mapping, method.toGenericString());
    }
    this.mappingRegistry.register(mapping, handler, method);
  }

  /**
   * Un-register the given mapping.
   * <p>This method may be invoked at runtime after initialization has completed.
   *
   * @param mapping the mapping to unregister
   */
  public void unregisterMapping(T mapping) {
    if (log.isTraceEnabled()) {
      log.trace("Unregister mapping \"{}\"", mapping);
    }
    this.mappingRegistry.unregister(mapping);
  }

  // Handler method detection

  /**
   * Detects handler methods at initialization.
   *
   * @see #initHandlerMethods
   */
  @Override
  public void afterPropertiesSet() {
    ApplicationContext context = obtainApplicationContext();
    this.context = context.unwrap(GenericApplicationContext.class);
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
    for (String beanName : getCandidateBeanNames()) {
      if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
        processCandidateBean(beanName);
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
  protected Set<String> getCandidateBeanNames() {
    return detectHandlerMethodsInAncestorContexts ?
           BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
           obtainApplicationContext().getBeanNamesForType(Object.class);
  }

  /**
   * Determine the type of the specified candidate bean and call
   * {@link #detectHandlerMethods} if identified as a handler type.
   * <p>This implementation avoids bean creation through checking
   * {@link cn.taketoday.beans.factory.BeanFactory#getType}
   * and calling {@link #detectHandlerMethods} with the bean name.
   *
   * @param beanName the name of the candidate bean
   * @see #isHandler
   * @see #detectHandlerMethods
   */
  protected void processCandidateBean(String beanName) {
    Class<?> beanType = null;
    try {
      beanType = obtainApplicationContext().getType(beanName);
    }
    catch (Throwable ex) {
      // An unresolvable bean type, probably from a lazy bean - let's ignore it.
      if (log.isTraceEnabled()) {
        log.trace("Could not resolve type for bean '{}'", beanName, ex);
      }
    }
    if (beanType != null && isHandler(beanType)) {
      detectHandlerMethods(beanName);
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
      Class<?> userType = ClassUtils.getUserClass(handlerType);
      Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
              method -> {
                try {
                  return getMappingForMethod(method, userType);
                }
                catch (Throwable ex) {
                  throw new IllegalStateException(
                          "Invalid mapping on handler class [" + userType.getName() + "]: " + method, ex);
                }
              });
      if (log.isTraceEnabled()) {
        log.trace(formatMappings(userType, methods));
      }
      else if (mappingsLogger.isDebugEnabled()) {
        mappingsLogger.debug(formatMappings(userType, methods));
      }

      for (Map.Entry<Method, T> entry : methods.entrySet()) {
        Method method = entry.getKey();
        T mapping = entry.getValue();
        Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
        registerHandlerMethod(handler, invocableMethod, mapping);
      }
    }
  }

  private String formatMappings(Class<?> userType, Map<Method, T> methods) {
    String packageName = ClassUtils.getPackageName(userType);
    String formattedType = (StringUtils.hasText(packageName) ?
                            Arrays.stream(packageName.split("\\."))
                                    .map(packageSegment -> packageSegment.substring(0, 1))
                                    .collect(Collectors.joining(".", "", "." + userType.getSimpleName())) :
                            userType.getSimpleName());
    Function<Method, String> methodFormatter = method -> Arrays.stream(method.getParameterTypes())
            .map(Class::getSimpleName)
            .collect(Collectors.joining(",", "(", ")"));
    return methods.entrySet().stream()
            .map(e -> {
              Method method = e.getKey();
              return e.getValue() + ": " + method.getName() + methodFormatter.apply(method);
            })
            .collect(Collectors.joining("\n\t", "\n\t" + formattedType + ":" + "\n\t", ""));
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
  protected void registerHandlerMethod(Object handler, Method method, T mapping) {
    this.mappingRegistry.register(mapping, handler, method);
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
      return new HandlerMethod(beanName,
              obtainApplicationContext().getAutowireCapableBeanFactory(),
              obtainApplicationContext(),
              method);
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
    if ((log.isTraceEnabled() && total == 0) || (log.isDebugEnabled() && total > 0)) {
      log.debug("{} mappings in {}", total, formatMappingName());
    }
  }

  // Handler method lookup

  /**
   * Look up a handler method for the given request.
   */
  @Nullable
  @Override
  protected HandlerMethod getHandlerInternal(RequestContext context) {
    String directLookupPath = context.getLookupPath().value();
    this.mappingRegistry.acquireReadLock();
    try {
      HandlerMethod handlerMethod = lookupHandlerMethod(directLookupPath, context);
      if (handlerMethod != null) {
        return handlerMethod.createWithResolvedBean();
      }
      return null;
    }
    finally {
      this.mappingRegistry.releaseReadLock();
    }
  }

  /**
   * Look up the best-matching handler method for the current request.
   * If multiple matches are found, the best match is selected.
   *
   * @param directLookupPath mapping lookup path within the current servlet mapping
   * @param request the current request
   * @return the best-matching handler method, or {@code null} if no match
   * @see #handleMatch(Match, String, RequestContext)
   * @see #handleNoMatch(Set, String, RequestContext)
   */
  @Nullable
  protected HandlerMethod lookupHandlerMethod(String directLookupPath, RequestContext request) {
    ArrayList<Match> matches = new ArrayList<>();
    List<T> directPathMatches = mappingRegistry.getMappingsByDirectPath(directLookupPath);
    if (directPathMatches != null) {
      addMatchingMappings(directPathMatches, matches, request);
    }
    if (matches.isEmpty()) {
      addMatchingMappings(mappingRegistry.getRegistrations().keySet(), matches, request);
    }
    if (matches.isEmpty()) {
      return handleNoMatch(mappingRegistry.getRegistrations().keySet(), directLookupPath, request);
    }
    else {
      Match bestMatch;
      if (matches.size() > 1) {
        var comparator = new MatchComparator(getMappingComparator(request));
        matches.sort(comparator);
        bestMatch = matches.get(0);
        if (log.isTraceEnabled()) {
          log.trace("{} matching mappings: {}", matches.size(), matches);
        }
        if (request.isPreFlightRequest()) {
          for (Match match : matches) {
            if (match.hasCorsConfig()) {
              return PREFLIGHT_AMBIGUOUS_MATCH;
            }
          }
        }
        else {
          Match secondBestMatch = matches.get(1);
          if (comparator.compare(bestMatch, secondBestMatch) == 0) {
            Method m1 = bestMatch.getHandlerMethod().getMethod();
            Method m2 = secondBestMatch.getHandlerMethod().getMethod();
            String uri = request.getRequestURI();
            throw new IllegalStateException(
                    "Ambiguous handler methods mapped for '" + uri + "': {" + m1 + ", " + m2 + "}");
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

  private void addMatchingMappings(Collection<T> mappings, List<Match> matches, RequestContext request) {
    Map<T, MappingRegistration<T>> registrations = mappingRegistry.getRegistrations();
    for (T mapping : mappings) {
      T match = getMatchingMapping(mapping, request);
      if (match != null) {
        matches.add(new Match(match, registrations.get(mapping)));
      }
    }
  }

  /**
   * Invoked when a matching mapping is found.
   *
   * @param bestMatch the best match metadata
   * @param directLookupPath mapping lookup path within the current servlet mapping
   * @param request the current request
   */
  protected void handleMatch(Match bestMatch, String directLookupPath, RequestContext request) {

  }

  /**
   * Invoked when no matching mapping is not found.
   *
   * @param mappings all registered mappings
   * @param lookupPath mapping lookup path within the current servlet mapping
   * @param request the current request
   */
  @Nullable
  protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, RequestContext request) {

    return null;
  }

  @Override
  protected boolean hasCorsConfigurationSource(Object handler) {
    return super.hasCorsConfigurationSource(handler) ||
            (handler instanceof HandlerMethod handlerMethod &&
                    this.mappingRegistry.getCorsConfiguration(handlerMethod) != null);
  }

  @Override
  protected CorsConfiguration getCorsConfiguration(Object handler, RequestContext request) {
    CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
    if (handler instanceof HandlerMethod handlerMethod) {
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
    if (handler instanceof HandlerMethod handlerMethod) {
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

    // get interceptor on class
    MergedAnnotations.from(controllerClass, SearchStrategy.TYPE_HIERARCHY)
            .stream(Interceptor.class)
            .forEach(interceptor -> {
              addInterceptors(ret, interceptor);
            });

    // HandlerInterceptor on a method
    MergedAnnotations.from(action, SearchStrategy.TYPE_HIERARCHY)
            .stream(Interceptor.class)
            .forEach(interceptor -> {
              addInterceptors(ret, interceptor);

              // exclude interceptors
              for (var exclude : interceptor.<HandlerInterceptor>getClassArray("exclude")) {
                ret.remove(context.getBean(exclude));
              }

              for (String excludeName : interceptor.getStringArray("excludeNames")) {
                ret.remove(context.getBean(excludeName, HandlerInterceptor.class));
              }
            });

    return ret;
  }

  private void addInterceptors(ArrayList<HandlerInterceptor> ret, MergedAnnotation<Interceptor> annotation) {
    var interceptors = annotation.<HandlerInterceptor>getClassValueArray();

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
          if (!context.containsBeanDefinition(interceptor, true)) {
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
        throw new IllegalStateException("The bean '" + includeName + "' is not a HandlerInterceptor");
      }
    }
  }

  private void registerInterceptorBean(Class<?> interceptor) {
    try {
      context.registerBean(interceptor);
    }
    catch (BeanDefinitionStoreException e) {
      throw new FrameworkConfigurationException(
              "Interceptor: [" + interceptor.getName() + "] register error", e);
    }
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
   * @param request the current HTTP servlet request
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

    private final HashMap<T, MappingRegistration<T>> registry = new HashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final LinkedMultiValueMap<String, T> pathLookup = new LinkedMultiValueMap<>();
    private final ConcurrentHashMap<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

    /**
     * Return all registrations.
     */
    public Map<T, MappingRegistration<T>> getRegistrations() {
      return this.registry;
    }

    /**
     * Return matches for the given URL path. Not thread-safe.
     *
     * @see #acquireReadLock()
     */
    @Nullable
    public List<T> getMappingsByDirectPath(String urlPath) {
      return pathLookup.get(urlPath);
    }

    /**
     * Return handler methods by mapping name. Thread-safe for concurrent use.
     */
    public List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
      return this.nameLookup.get(mappingName);
    }

    /**
     * Return CORS configuration. Thread-safe for concurrent use.
     */
    @Nullable
    public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
      HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
      return corsLookup.get(original != null ? original : handlerMethod);
    }

    /**
     * Acquire the read lock when using getMappings and getMappingsByUrl.
     */
    public void acquireReadLock() {
      readWriteLock.readLock().lock();
    }

    /**
     * Release the read lock after using getMappings and getMappingsByUrl.
     */
    public void releaseReadLock() {
      readWriteLock.readLock().unlock();
    }

    public void register(T mapping, Object handler, Method method) {
      readWriteLock.writeLock().lock();
      try {
        HandlerMethod handlerMethod = createHandlerMethod(handler, method);
        validateMethodMapping(handlerMethod, mapping);

        Set<String> directPaths = getDirectPaths(mapping);
        for (String path : directPaths) {
          pathLookup.add(path, mapping);
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
          corsLookup.put(handlerMethod, corsConfig);
        }

        registry.put(mapping, new MappingRegistration<>(
                mapping, handlerMethod, directPaths, name, corsConfig != null));
      }
      finally {
        readWriteLock.writeLock().unlock();
      }
    }

    private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
      MappingRegistration<T> registration = registry.get(mapping);
      HandlerMethod existingHandlerMethod = registration != null ? registration.handlerMethod() : null;
      if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
        throw new IllegalStateException(
                "Ambiguous mapping. Cannot map '" + handlerMethod.getBean() + "' method \n" +
                        handlerMethod + "\nto " + mapping + ": There is already '" +
                        existingHandlerMethod.getBean() + "' bean method\n" + existingHandlerMethod + " mapped.");
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
      readWriteLock.writeLock().lock();
      try {
        MappingRegistration<T> registration = registry.remove(mapping);
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

        corsLookup.remove(registration.handlerMethod);
      }
      finally {
        readWriteLock.writeLock().unlock();
      }
    }

    private void removeMappingName(MappingRegistration<T> definition) {
      String name = definition.mappingName;
      if (name == null) {
        return;
      }
      HandlerMethod handlerMethod = definition.handlerMethod();
      List<HandlerMethod> oldList = nameLookup.get(name);
      if (oldList == null) {
        return;
      }
      if (oldList.size() <= 1) {
        nameLookup.remove(name);
        return;
      }
      List<HandlerMethod> newList = new ArrayList<>(oldList.size() - 1);
      for (HandlerMethod current : oldList) {
        if (!current.equals(handlerMethod)) {
          newList.add(current);
        }
      }
      nameLookup.put(name, newList);
    }

    @Override
    protected HandlerInterceptor[] createValue(Method method, @Nullable HandlerMethod handlerMethod) {
      Assert.state(handlerMethod != null, "No HandlerMethod");
      Class<?> controllerClass = handlerMethod.getBeanType();
      List<HandlerInterceptor> interceptors = getInterceptors(controllerClass, method);
      return interceptors.toArray(HandlerInterceptor.EMPTY_ARRAY);
    }

    public HandlerInterceptor[] getHandlerInterceptors(HandlerMethod handlerMethod) {
      Method method = handlerMethod.getMethod();
      return get(method, handlerMethod);
    }
  }

  record MappingRegistration<T>(
          T mapping, HandlerMethod handlerMethod,
          Set<String> directPaths, @Nullable String mappingName, boolean hasCorsConfig) {

    MappingRegistration(T mapping, HandlerMethod handlerMethod,
            @Nullable Set<String> directPaths, @Nullable String mappingName, boolean hasCorsConfig) {
      Assert.notNull(mapping, "Mapping is required");
      Assert.notNull(handlerMethod, "HandlerMethod is required");
      this.mapping = mapping;
      this.handlerMethod = handlerMethod;
      this.directPaths = directPaths != null ? directPaths : Collections.emptySet();
      this.mappingName = mappingName;
      this.hasCorsConfig = hasCorsConfig;
    }
  }

  /**
   * A thin wrapper around a matched HandlerMethod and its mapping, for the purpose of
   * comparing the best match with a comparator in the context of the current request.
   */
  protected final class Match {

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

  private class MatchComparator implements Comparator<Match> {

    private final Comparator<T> comparator;

    public MatchComparator(Comparator<T> comparator) {
      this.comparator = comparator;
    }

    @Override
    public int compare(Match match1, Match match2) {
      return this.comparator.compare(match1.mapping, match2.mapping);
    }
  }

  private static class EmptyHandler {

    @SuppressWarnings("unused")
    public void handle() {
      throw new UnsupportedOperationException("Not implemented");
    }
  }

}
