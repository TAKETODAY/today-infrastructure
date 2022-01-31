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
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.util.pattern.PathPattern;

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
  private final LinkedHashMap<PathPattern, Object> handlerMap = new LinkedHashMap<>();

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

    Object handler;
    RequestPath path = request.getLookupPath();
    handler = lookupHandler(path, request);

    if (handler == null) {
      // We need to care for the default handler directly, since we need to
      // expose the PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE for it as well.
      Object rawHandler = null;
      String lookupPath = initLookupPath(request);
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
        handler = rawHandler;
      }
    }
    return handler;
  }

  /**
   * Look up a handler instance for the given URL path.
   *
   * @param lookupPath the String lookupPath for checking direct hits
   * @param request current HTTP request
   * @return a matching handler, or {@code null} if not found
   * @since 4.0
   */
  @Nullable
  protected Object lookupHandler(RequestPath lookupPath, RequestContext request) {
    List<PathPattern> matches = null;
    for (PathPattern pattern : this.handlerMap.keySet()) {
      if (pattern.matches(lookupPath)) {
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
    PathContainer pathWithinMapping = pattern.extractPathWithinPattern(lookupPath);
    PathPattern.PathMatchInfo matchInfo = pattern.matchAndExtract(lookupPath);
    Assert.notNull(matchInfo, "Expected a match");

    Object handler = this.handlerMap.get(pattern);

    // Bean name or resolved handler?
    if (handler instanceof String handlerName) {
      handler = obtainApplicationContext().getBean(handlerName);
    }

    validateHandler(handler, request);

    request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
    request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
    request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
    request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, matchInfo.getUriVariables());

    return handler;
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
   */
  public void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
    Assert.notNull(urlPath, "URL path must not be null");
    Assert.notNull(handler, "Handler object must not be null");
    Object resolvedHandler = handler;

    // Parse path pattern
    urlPath = prependLeadingSlash(urlPath);
    PathPattern pattern = getPatternParser().parse(urlPath);
    if (this.handlerMap.containsKey(pattern)) {
      Object existingHandler = this.handlerMap.get(pattern);
      if (existingHandler != null && existingHandler != resolvedHandler) {
        throw new IllegalStateException(
                "Cannot map " + getHandlerDescription(handler) + " to [" + urlPath + "]: " +
                        "there is already " + getHandlerDescription(existingHandler) + " mapped.");
      }
    }

    // Eagerly resolve handler if referencing singleton via name.
    if (!this.lazyInitHandlers && handler instanceof String handlerName) {
      if (obtainApplicationContext().isSingleton(handlerName)) {
        resolvedHandler = obtainApplicationContext().getBean(handlerName);
      }
    }

    // Register resolved handler
    this.handlerMap.put(pattern, resolvedHandler);
    if (log.isTraceEnabled()) {
      log.trace("Mapped [{}] onto {}", urlPath, getHandlerDescription(handler));
    }
  }

  private String getHandlerDescription(Object handler) {
    return handler instanceof String ? "'" + handler + "'" : handler.toString();
  }

  private static String prependLeadingSlash(String pattern) {
    if (StringUtils.isNotEmpty(pattern) && !pattern.startsWith("/")) {
      return "/" + pattern;
    }
    else {
      return pattern;
    }
  }

  /**
   * Return a read-only view of registered path patterns and handlers which may
   * may be an actual handler instance or the bean name of lazily initialized
   * handler.
   */
  public final Map<PathPattern, Object> getHandlerMap() {
    return Collections.unmodifiableMap(this.handlerMap);
  }

}
