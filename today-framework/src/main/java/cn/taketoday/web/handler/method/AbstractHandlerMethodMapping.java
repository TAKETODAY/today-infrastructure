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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.cors.CorsUtils;
import cn.taketoday.web.handler.HandlerMethodMappingNamingStrategy;
import cn.taketoday.web.registry.AbstractHandlerRegistry;
import cn.taketoday.web.registry.HandlerRegistry;
import jakarta.servlet.ServletException;

/**
 * Abstract base class for {@link HandlerRegistry} implementations that define
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
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerRegistry implements InitializingBean {

  /**
   * Bean name prefix for target beans behind scoped proxies. Used to exclude those
   * targets from handler method detection, in favor of the corresponding proxies.
   * <p>We're not checking the autowire-candidate status here, which is how the
   * proxy target filtering problem is being handled at the autowiring level,
   * since autowire-candidate may have been turned to {@code false} for other
   * reasons, while still expecting the bean to be eligible for handler methods.
   * <p>Originally defined in {@link cn.taketoday.aop.scope.ScopedProxyUtils}
   * but duplicated here to avoid a hard dependency on the spring-aop module.
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

  private final MappingRegistry mappingRegistry = new MappingRegistry();

  /**
   * Whether to detect handler methods in beans in ancestor ApplicationContexts.
   * <p>Default is "false": Only beans in the current ApplicationContext are
   * considered, i.e. only in the context that this HandlerMapping itself
   * is defined in (typically the current DispatcherServlet's context).
   * <p>Switch this flag on to detect handler beans in ancestor contexts
   * (typically the Spring root WebApplicationContext) as well.
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
    return (this.detectHandlerMethodsInAncestorContexts ?
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
            obtainApplicationContext().getBeanNamesForType(Object.class));
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
        log.trace("Could not resolve type for bean '" + beanName + "'", ex);
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
    Class<?> handlerType = (handler instanceof String beanName ?
                            obtainApplicationContext().getType(beanName) : handler.getClass());

    if (handlerType != null) {
      Class<?> userType = ClassUtils.getUserClass(handlerType);
      Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
              method -> {
                try {
                  return getMappingForMethod(method, userType);
                }
                catch (Throwable ex) {
                  throw new IllegalStateException("Invalid mapping on handler class [" +
                          userType.getName() + "]: " + method, ex);
                }
              });
      if (log.isTraceEnabled()) {
        log.trace(formatMappings(userType, methods));
      }
      else if (mappingsLogger.isDebugEnabled()) {
        mappingsLogger.debug(formatMappings(userType, methods));
      }
      methods.forEach((method, mapping) -> {
        Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
        registerHandlerMethod(handler, invocableMethod, mapping);
      });
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
  protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
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
  protected Object lookupInternal(RequestContext context) {
    String lookupPath = initLookupPath(request);
    this.mappingRegistry.acquireReadLock();
    try {
      HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, context);
      return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
    }
    finally {
      this.mappingRegistry.releaseReadLock();
    }
  }

  /**
   * Look up the best-matching handler method for the current request.
   * If multiple matches are found, the best match is selected.
   *
   * @param lookupPath mapping lookup path within the current servlet mapping
   * @param request the current request
   * @return the best-matching handler method, or {@code null} if no match
   * @see #handleMatch(Object, String, RequestContext)
   * @see #handleNoMatch(Set, String, RequestContext)
   */
  @Nullable
  protected HandlerMethod lookupHandlerMethod(String lookupPath, RequestContext request) throws Exception {
    List<Match> matches = new ArrayList<>();
    List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(lookupPath);
    if (directPathMatches != null) {
      addMatchingMappings(directPathMatches, matches, request);
    }
    if (matches.isEmpty()) {
      addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, request);
    }
    if (!matches.isEmpty()) {
      Match bestMatch = matches.get(0);
      if (matches.size() > 1) {
        Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
        matches.sort(comparator);
        bestMatch = matches.get(0);
        if (log.isTraceEnabled()) {
          log.trace(matches.size() + " matching mappings: " + matches);
        }
        if (CorsUtils.isPreFlightRequest(request)) {
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
      request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, bestMatch.getHandlerMethod());
      handleMatch(bestMatch.mapping, lookupPath, request);
      return bestMatch.getHandlerMethod();
    }
    else {
      return handleNoMatch(this.mappingRegistry.getRegistrations().keySet(), lookupPath, request);
    }
  }

  private void addMatchingMappings(Collection<T> mappings, List<Match> matches, RequestContext request) {
    for (T mapping : mappings) {
      T match = getMatchingMapping(mapping, request);
      if (match != null) {
        matches.add(new Match(match, this.mappingRegistry.getRegistrations().get(mapping)));
      }
    }
  }

  /**
   * Invoked when a matching mapping is found.
   *
   * @param mapping the matching mapping
   * @param lookupPath mapping lookup path within the current servlet mapping
   * @param request the current request
   */
  protected void handleMatch(T mapping, String lookupPath, RequestContext request) {
    request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
  }

  /**
   * Invoked when no matching mapping is not found.
   *
   * @param mappings all registered mappings
   * @param lookupPath mapping lookup path within the current servlet mapping
   * @param request the current request
   * @throws ServletException in case of errors
   */
  @Nullable
  protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, RequestContext request)
          throws Exception {

    return null;
  }

  @Override
  protected boolean hasCorsConfigurationSource(Object handler) {
    return super.hasCorsConfigurationSource(handler) ||
            (handler instanceof HandlerMethod handerMethod &&
                    this.mappingRegistry.getCorsConfiguration(handerMethod) != null);
  }

  @Override
  protected CorsConfiguration getCorsConfiguration(Object handler, RequestContext request) {
    CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
    if (handler instanceof HandlerMethod handlerMethod) {
      if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
        return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
      }
      else {
        CorsConfiguration corsConfigFromMethod = this.mappingRegistry.getCorsConfiguration(handlerMethod);
        corsConfig = (corsConfig != null ? corsConfig.combine(corsConfigFromMethod) : corsConfigFromMethod);
      }
    }
    return corsConfig;
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
   * Extract and return the URL paths contained in the supplied mapping.
   *
   * @deprecated as of 5.3 in favor of providing non-pattern mappings via
   * {@link #getDirectPaths(Object)} instead
   */
  @Deprecated
  protected Set<String> getMappingPathPatterns(T mapping) {
    return Collections.emptySet();
  }

  /**
   * Return the request mapping paths that are not patterns.
   */
  protected Set<String> getDirectPaths(T mapping) {
    Set<String> urls = Collections.emptySet();
    for (String path : getMappingPathPatterns(mapping)) {
      if (!getPathMatcher().isPattern(path)) {
        urls = (urls.isEmpty() ? new HashSet<>(1) : urls);
        urls.add(path);
      }
    }
    return urls;
  }

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
  class MappingRegistry {

    private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

    private final MultiValueMap<String, T> pathLookup = new LinkedMultiValueMap<>();

    private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();

    private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

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
      return this.pathLookup.get(urlPath);
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
      return this.corsLookup.get(original != null ? original : handlerMethod);
    }

    /**
     * Acquire the read lock when using getMappings and getMappingsByUrl.
     */
    public void acquireReadLock() {
      this.readWriteLock.readLock().lock();
    }

    /**
     * Release the read lock after using getMappings and getMappingsByUrl.
     */
    public void releaseReadLock() {
      this.readWriteLock.readLock().unlock();
    }

    public void register(T mapping, Object handler, Method method) {
      this.readWriteLock.writeLock().lock();
      try {
        HandlerMethod handlerMethod = createHandlerMethod(handler, method);
        validateMethodMapping(handlerMethod, mapping);

        Set<String> directPaths = AbstractHandlerMethodMapping.this.getDirectPaths(mapping);
        for (String path : directPaths) {
          this.pathLookup.add(path, mapping);
        }

        String name = null;
        if (getNamingStrategy() != null) {
          name = getNamingStrategy().getName(handlerMethod, mapping);
          addMappingName(name, handlerMethod);
        }

        CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
        if (corsConfig != null) {
          corsConfig.validateAllowCredentials();
          this.corsLookup.put(handlerMethod, corsConfig);
        }

        this.registry.put(mapping,
                new MappingRegistration<>(mapping, handlerMethod, directPaths, name, corsConfig != null));
      }
      finally {
        this.readWriteLock.writeLock().unlock();
      }
    }

    private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
      MappingRegistration<T> registration = this.registry.get(mapping);
      HandlerMethod existingHandlerMethod = (registration != null ? registration.getHandlerMethod() : null);
      if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
        throw new IllegalStateException(
                "Ambiguous mapping. Cannot map '" + handlerMethod.getBean() + "' method \n" +
                        handlerMethod + "\nto " + mapping + ": There is already '" +
                        existingHandlerMethod.getBean() + "' bean method\n" + existingHandlerMethod + " mapped.");
      }
    }

    private void addMappingName(String name, HandlerMethod handlerMethod) {
      List<HandlerMethod> oldList = this.nameLookup.get(name);
      if (oldList == null) {
        oldList = Collections.emptyList();
      }

      for (HandlerMethod current : oldList) {
        if (handlerMethod.equals(current)) {
          return;
        }
      }

      List<HandlerMethod> newList = new ArrayList<>(oldList.size() + 1);
      newList.addAll(oldList);
      newList.add(handlerMethod);
      this.nameLookup.put(name, newList);
    }

    public void unregister(T mapping) {
      this.readWriteLock.writeLock().lock();
      try {
        MappingRegistration<T> registration = this.registry.remove(mapping);
        if (registration == null) {
          return;
        }

        for (String path : registration.getDirectPaths()) {
          List<T> mappings = this.pathLookup.get(path);
          if (mappings != null) {
            mappings.remove(registration.getMapping());
            if (mappings.isEmpty()) {
              this.pathLookup.remove(path);
            }
          }
        }

        removeMappingName(registration);

        this.corsLookup.remove(registration.getHandlerMethod());
      }
      finally {
        this.readWriteLock.writeLock().unlock();
      }
    }

    private void removeMappingName(MappingRegistration<T> definition) {
      String name = definition.getMappingName();
      if (name == null) {
        return;
      }
      HandlerMethod handlerMethod = definition.getHandlerMethod();
      List<HandlerMethod> oldList = this.nameLookup.get(name);
      if (oldList == null) {
        return;
      }
      if (oldList.size() <= 1) {
        this.nameLookup.remove(name);
        return;
      }
      List<HandlerMethod> newList = new ArrayList<>(oldList.size() - 1);
      for (HandlerMethod current : oldList) {
        if (!current.equals(handlerMethod)) {
          newList.add(current);
        }
      }
      this.nameLookup.put(name, newList);
    }
  }

  static class MappingRegistration<T> {

    private final T mapping;

    private final HandlerMethod handlerMethod;

    private final Set<String> directPaths;

    @Nullable
    private final String mappingName;

    private final boolean corsConfig;

    public MappingRegistration(T mapping, HandlerMethod handlerMethod,
            @Nullable Set<String> directPaths, @Nullable String mappingName, boolean corsConfig) {

      Assert.notNull(mapping, "Mapping must not be null");
      Assert.notNull(handlerMethod, "HandlerMethod must not be null");
      this.mapping = mapping;
      this.handlerMethod = handlerMethod;
      this.directPaths = (directPaths != null ? directPaths : Collections.emptySet());
      this.mappingName = mappingName;
      this.corsConfig = corsConfig;
    }

    public T getMapping() {
      return this.mapping;
    }

    public HandlerMethod getHandlerMethod() {
      return this.handlerMethod;
    }

    public Set<String> getDirectPaths() {
      return this.directPaths;
    }

    @Nullable
    public String getMappingName() {
      return this.mappingName;
    }

    public boolean hasCorsConfig() {
      return this.corsConfig;
    }
  }

  /**
   * A thin wrapper around a matched HandlerMethod and its mapping, for the purpose of
   * comparing the best match with a comparator in the context of the current request.
   */
  private class Match {

    private final T mapping;

    private final MappingRegistration<T> registration;

    public Match(T mapping, MappingRegistration<T> registration) {
      this.mapping = mapping;
      this.registration = registration;
    }

    public HandlerMethod getHandlerMethod() {
      return this.registration.getHandlerMethod();
    }

    public boolean hasCorsConfig() {
      return this.registration.hasCorsConfig();
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
