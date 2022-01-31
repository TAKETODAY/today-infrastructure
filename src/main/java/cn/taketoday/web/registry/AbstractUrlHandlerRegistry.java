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
package cn.taketoday.web.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.util.RequestPathUtils;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Abstract base class for URL-mapped {@link HandlerRegistry} implementations.
 *
 * <p>Supports literal matches and pattern matches such as "/test/*", "/test/**",
 * and others. For details on pattern syntax refer to {@link PathPattern}
 *
 * <p>All path patterns are checked in order to find the most exact match for the
 * current request path where the "most exact" is the longest path pattern that
 * matches the current request path.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author TODAY 2020/12/23 15:56
 * @since 3.0
 */
public abstract class AbstractUrlHandlerRegistry extends AbstractHandlerRegistry {

  private Object rootHandler;

  private boolean useTrailingSlashMatch = true;

  // @since 4.0
  private boolean lazyInitHandlers = false;

  // @since 4.0
  private final LinkedHashMap<String, Object> handlerMap = new LinkedHashMap<>();

  // @since 4.0
  private final LinkedHashMap<PathPattern, Object> pathPatternHandlerMap = new LinkedHashMap<>();

  @Override
  public void setPatternParser(PathPatternParser patternParser) {
    Assert.state(this.handlerMap.isEmpty(),
            "PathPatternParser must be set before the initialization of " +
                    "the handler map via ApplicationContextAware#setApplicationContext.");
    super.setPatternParser(patternParser);
  }

  /**
   * Set the root handler for this handler mapping, that is,
   * the handler to be registered for the root path ("/").
   * <p>Default is {@code null}, indicating no root handler.
   */
  public void setRootHandler(Object rootHandler) {
    this.rootHandler = rootHandler;
  }

  /**
   * Return the root handler for this handler mapping (registered for "/"),
   * or {@code null} if none.
   */
  @Nullable
  public Object getRootHandler() {
    return this.rootHandler;
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   * If enabled a URL pattern such as "/users" also matches to "/users/".
   * <p>The default value is {@code false}.
   */
  public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
    this.useTrailingSlashMatch = useTrailingSlashMatch;
    getPatternParser().setMatchOptionalTrailingSeparator(useTrailingSlashMatch);
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   */
  public boolean useTrailingSlashMatch() {
    return this.useTrailingSlashMatch;
  }

  /**
   * Set whether to lazily initialize handlers. Only applicable to
   * singleton handlers, as prototypes are always lazily initialized.
   * Default is "false", as eager initialization allows for more efficiency
   * through referencing the controller objects directly.
   * <p>If you want to allow your controllers to be lazily initialized,
   * make them "lazy-init" and set this flag to true. Just making them
   * "lazy-init" will not work, as they are initialized through the
   * references from the handler mapping in this case.
   *
   * @since 4.0
   */
  public void setLazyInitHandlers(boolean lazyInitHandlers) {
    this.lazyInitHandlers = lazyInitHandlers;
  }

  /**
   * Look up a handler for the URL path of the given request.
   *
   * @param request current HTTP request context
   * @return the handler instance, or {@code null} if none found
   * @since 4.0
   */
  @Nullable
  @Override
  protected Object lookupInternal(RequestContext request) {
    String lookupPath = initLookupPath(request);
    Object handler;
    RequestPath path = RequestPathUtils.getParsedRequestPath(request);
    handler = lookupHandler(path, lookupPath, request);

    if (handler == null) {
      // We need to care for the default handler directly, since we need to
      // expose the PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE for it as well.
      Object rawHandler = null;
      if (StringUtils.matchesCharacter(lookupPath, '/')) {
        rawHandler = getRootHandler();
      }
      if (rawHandler == null) {
        rawHandler = getDefaultHandler();
      }
      if (rawHandler != null) {
        // Bean name or resolved handler?
        if (rawHandler instanceof String handlerName) {
          rawHandler = obtainApplicationContext().getBean(handlerName);
        }
        validateHandler(rawHandler, request);
        handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
      }
    }
    return handler;
  }

  /**
   * Look up a handler instance for the given URL path.
   *
   * @param path the parsed RequestPath
   * @param lookupPath the String lookupPath for checking direct hits
   * @param request current HTTP request
   * @return a matching handler, or {@code null} if not found
   * @since 4.0
   */
  @Nullable
  protected Object lookupHandler(
          RequestPath path, String lookupPath, RequestContext request) {

    Object handler = getDirectMatch(lookupPath, request);
    if (handler != null) {
      return handler;
    }

    // Pattern match?
    List<PathPattern> matches = null;
    for (PathPattern pattern : this.pathPatternHandlerMap.keySet()) {
      if (pattern.matches(path.pathWithinApplication())) {
        matches = (matches != null ? matches : new ArrayList<>());
        matches.add(pattern);
      }
    }
    if (matches == null) {
      return null;
    }
    if (matches.size() > 1) {
      matches.sort(PathPattern.SPECIFICITY_COMPARATOR);
      if (log.isTraceEnabled()) {
        log.trace("Matching patterns {}", matches);
      }
    }
    PathPattern pattern = matches.get(0);
    handler = this.pathPatternHandlerMap.get(pattern);
    if (handler instanceof String handlerName) {
      handler = obtainApplicationContext().getBean(handlerName);
    }
    validateHandler(handler, request);
    PathContainer pathWithinMapping = pattern.extractPathWithinPattern(path.pathWithinApplication());
    return buildPathExposingHandler(handler, pattern.getPatternString(), pathWithinMapping.value(), null);
  }

  // @since 4.0
  @Nullable
  private Object getDirectMatch(String urlPath, RequestContext request) {
    Object handler = this.handlerMap.get(urlPath);
    if (handler != null) {
      // Bean name or resolved handler?
      if (handler instanceof String handlerName) {
        handler = obtainApplicationContext().getBean(handlerName);
      }
      validateHandler(handler, request);
      return buildPathExposingHandler(handler, urlPath, urlPath, null);
    }
    return null;
  }

  /**
   * Validate the given handler against the current request.
   * <p>The default implementation is empty. Can be overridden in subclasses,
   * for example to enforce specific preconditions expressed in URL mappings.
   *
   * @param handler the handler object to validate
   * @param request current HTTP request
   */
  protected void validateHandler(Object handler, RequestContext request) { }

  /**
   * Build a handler object for the given raw handler, exposing the actual
   * handler, the {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}, as well as
   * the {@link #URI_TEMPLATE_VARIABLES_ATTRIBUTE} before executing the handler.
   * <p>The default implementation  add into InterceptableRequestHandler
   * with a special interceptor that exposes the path attribute and URI
   * template variables
   *
   * @param rawHandler the raw handler to expose
   * @param pathWithinMapping the path to expose before executing the handler
   * @param uriTemplateVariables the URI template variables, can be {@code null} if no variables found
   * @return the final handler object
   */
  protected Object buildPathExposingHandler(
          Object rawHandler, String bestMatchingPattern,
          String pathWithinMapping, @Nullable Map<String, String> uriTemplateVariables) {

//    if (rawHandler instanceof InterceptableRequestHandler interceptable) {
//      interceptable.addInterceptors(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
//      if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
//        interceptable.addInterceptors(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
//      }
//    }
    return rawHandler;
  }

  /**
   * Expose the path within the current mapping as request attribute.
   *
   * @param pathWithinMapping the path within the current mapping
   * @param request the request to expose the path to
   * @see #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
   * @since 4.0
   */
  protected void exposePathWithinMapping(
          String bestMatchingPattern, String pathWithinMapping, RequestContext request) {
    request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatchingPattern);
    request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
  }

  /**
   * Expose the URI templates variables as request attribute.
   *
   * @param uriTemplateVariables the URI template variables
   * @param request the request to expose the path to
   * @see #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
   * @since 4.0
   */
  protected void exposeUriTemplateVariables(
          Map<String, String> uriTemplateVariables, RequestContext request) {
    request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
  }

  /**
   * Register the specified handler for the given URL paths.
   *
   * @param urlPaths the URLs that the bean should be mapped to
   * @param beanName the name of the handler bean
   * @throws BeansException if the handler couldn't be registered
   * @throws IllegalStateException if there is a conflicting handler registered
   * @since 4.0
   */
  protected void registerHandler(
          String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
    Assert.notNull(urlPaths, "URL path array must not be null");
    for (String urlPath : urlPaths) {
      registerHandler(urlPath, beanName);
    }
  }

  /**
   * Register the specified handler for the given URL path.
   *
   * @param urlPath the URL the bean should be mapped to
   * @param handler the handler instance or handler bean name String
   * (a bean name will automatically be resolved into the corresponding handler bean)
   * @throws BeansException if the handler couldn't be registered
   * @throws IllegalStateException if there is a conflicting handler registered
   * @since 4.0
   */
  protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
    Assert.notNull(urlPath, "URL path must not be null");
    Assert.notNull(handler, "Handler object must not be null");
    Object resolvedHandler = handler;

    // Eagerly resolve handler if referencing singleton via name.
    if (!this.lazyInitHandlers && handler instanceof String handlerName) {
      ApplicationContext applicationContext = obtainApplicationContext();
      if (applicationContext.isSingleton(handlerName)) {
        resolvedHandler = applicationContext.getBean(handlerName);
      }
    }

    Object mappedHandler = this.handlerMap.get(urlPath);
    if (mappedHandler != null) {
      if (mappedHandler != resolvedHandler) {
        throw new IllegalStateException(
                "Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
                        "]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
      }
    }
    else {
      if (urlPath.equals("/")) {
        if (log.isTraceEnabled()) {
          log.trace("Root mapping to {}", getHandlerDescription(handler));
        }
        setRootHandler(resolvedHandler);
      }
      else if (urlPath.equals("/*")) {
        if (log.isTraceEnabled()) {
          log.trace("Default mapping to {}", getHandlerDescription(handler));
        }
        setDefaultHandler(resolvedHandler);
      }
      else {
        this.handlerMap.put(urlPath, resolvedHandler);
        this.pathPatternHandlerMap.put(getPatternParser().parse(urlPath), resolvedHandler);
        if (log.isTraceEnabled()) {
          log.trace("Mapped [{}] onto {}", urlPath, getHandlerDescription(handler));
        }
      }
    }
  }

  private String getHandlerDescription(Object handler) {
    return handler instanceof String ? "'" + handler + "'" : handler.toString();
  }

  /**
   * Return the handler mappings as a read-only Map, with the registered path
   * or pattern as key and the handler object (or handler bean name in case of
   * a lazy-init handler), as value.
   *
   * @see #getDefaultHandler()
   * @since 4.0
   */
  public final Map<String, Object> getHandlerMap() {
    return Collections.unmodifiableMap(this.handlerMap);
  }

  /**
   * Identical to {@link #getHandlerMap()} but populated
   *
   * @since 4.0
   */
  public final Map<PathPattern, Object> getPathPatternHandlerMap() {
    return this.pathPatternHandlerMap.isEmpty()
           ? Collections.emptyMap() : Collections.unmodifiableMap(this.pathPatternHandlerMap);
  }

  /**
   * Indicates whether this handler mapping support type-level mappings. Default to {@code false}.
   *
   * @since 4.0
   */
  protected boolean supportsTypeLevelMappings() {
    return false;
  }

  /**
   * Special interceptor for exposing the
   * {@link AbstractUrlHandlerRegistry#PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE} attribute.
   *
   * @see AbstractUrlHandlerRegistry#exposePathWithinMapping
   */
  private class PathExposingHandlerInterceptor implements HandlerInterceptor {

    private final String bestMatchingPattern;

    private final String pathWithinMapping;

    public PathExposingHandlerInterceptor(String bestMatchingPattern, String pathWithinMapping) {
      this.bestMatchingPattern = bestMatchingPattern;
      this.pathWithinMapping = pathWithinMapping;
    }

    @Override
    public boolean beforeProcess(RequestContext request, Object handler) {
      exposePathWithinMapping(this.bestMatchingPattern, this.pathWithinMapping, request);
      request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
      request.setAttribute(INTROSPECT_TYPE_LEVEL_MAPPING, supportsTypeLevelMappings());
      return true;
    }

  }

  /**
   * Special interceptor for exposing the
   * {@link AbstractUrlHandlerRegistry#URI_TEMPLATE_VARIABLES_ATTRIBUTE} attribute.
   *
   * @see AbstractUrlHandlerRegistry#exposePathWithinMapping
   */
  private class UriTemplateVariablesHandlerInterceptor implements HandlerInterceptor {

    private final Map<String, String> uriTemplateVariables;

    public UriTemplateVariablesHandlerInterceptor(Map<String, String> uriTemplateVariables) {
      this.uriTemplateVariables = uriTemplateVariables;
    }

    @Override
    public boolean beforeProcess(RequestContext context, Object handler) {
      exposeUriTemplateVariables(this.uriTemplateVariables, context);
      return true;
    }
  }

}
