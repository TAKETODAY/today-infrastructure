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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.beans.BeansException;
import infra.context.ApplicationContext;
import infra.http.server.PathContainer;
import infra.lang.Assert;
import infra.util.StringUtils;
import infra.web.HandlerMapping;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.util.pattern.PathPattern;

/**
 * Abstract base class for URL-mapped {@link HandlerMapping} implementations.
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2020/12/23 15:56
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping {

  @Nullable
  private Object rootHandler;

  // @since 4.0
  private boolean lazyInitHandlers = false;

  // @since 4.0
  private final LinkedHashMap<String, Object> handlerMap = new LinkedHashMap<>();

  // @since 4.0
  private final LinkedHashMap<PathPattern, Object> pathPatternHandlerMap = new LinkedHashMap<>();

  /**
   * Set the root handler for this handler mapping, that is,
   * the handler to be registered for the root path ("/").
   * <p>Default is {@code null}, indicating no root handler.
   */
  public void setRootHandler(@Nullable Object rootHandler) {
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
  @SuppressWarnings("NullAway")
  protected Object getHandlerInternal(RequestContext request) {
    Object handler = lookupHandler(request);
    if (handler == null) {
      // We need to care for the default handler directly
      Object rawHandler = null;
      String lookupPath = request.getRequestPath().value();
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
   * @param request current HTTP request
   * @return a matching handler, or {@code null} if not found
   * @since 4.0
   */
  @Nullable
  @SuppressWarnings("NullAway")
  protected Object lookupHandler(RequestContext request) {
    PathContainer lookupPath = request.getRequestPath();

    String requestPath = lookupPath.value();
    Object handler = getDirectMatch(requestPath, request);
    if (handler != null) {
      var matchingMetadata = new HandlerMatchingMetadata(
              handler, requestPath, lookupPath, null, getPatternParser());
      request.setMatchingMetadata(matchingMetadata);
      return handler;
    }

    ArrayList<PathPattern> matches = null;
    for (PathPattern pattern : pathPatternHandlerMap.keySet()) {
      if (pattern.matches(lookupPath)) {
        if (matches == null) {
          matches = new ArrayList<>();
        }
        matches.add(pattern);
      }
    }
    if (matches == null) {
      return null;
    }
    if (matches.size() > 1) {
      matches.sort(PathPattern.SPECIFICITY_COMPARATOR);
      if (logger.isTraceEnabled()) {
        logger.trace("Matching patterns {}", matches);
      }
    }

    PathPattern bestMatchingPattern = matches.get(0);
    handler = pathPatternHandlerMap.get(bestMatchingPattern);

    // Bean name or resolved handler?
    if (handler instanceof String handlerName) {
      handler = obtainApplicationContext().getBean(handlerName);
    }

    validateHandler(handler, request);

    var matchingMetadata = new HandlerMatchingMetadata(
            handler, requestPath, lookupPath, bestMatchingPattern, getPatternParser());
    request.setMatchingMetadata(matchingMetadata);
    return handler;
  }

  @Nullable
  private Object getDirectMatch(String urlPath, RequestContext request) {
    Object handler = handlerMap.get(urlPath);
    if (handler != null) {
      // Bean name or resolved handler?
      if (handler instanceof String handlerName) {
        handler = obtainApplicationContext().getBean(handlerName);
      }
      validateHandler(handler, request);
      return handler;
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
  protected void validateHandler(@Nullable Object handler, RequestContext request) {

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
  protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
    Assert.notNull(urlPaths, "URL path array is required");
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
    Assert.notNull(urlPath, "URL path is required");
    Assert.notNull(handler, "Handler object is required");
    Object resolvedHandler = handler;

    // Eagerly resolve handler if referencing singleton via name.
    if (!this.lazyInitHandlers && handler instanceof String handlerName) {
      ApplicationContext beanFactory = obtainApplicationContext();
      if (beanFactory.isSingleton(handlerName)) {
        resolvedHandler = beanFactory.getBean(handlerName);
      }
    }

    Object mappedHandler = handlerMap.get(urlPath);
    if (mappedHandler != null) {
      if (mappedHandler != resolvedHandler) {
        throw new IllegalStateException("Cannot map %s to URL path [%s]: There is already %s mapped."
                .formatted(getHandlerDescription(handler), urlPath, getHandlerDescription(mappedHandler)));
      }
    }
    else {
      if (urlPath.equals("/")) {
        if (logger.isTraceEnabled()) {
          logger.trace("Root mapping to {}", getHandlerDescription(handler));
        }
        setRootHandler(resolvedHandler);
      }
      else if (urlPath.equals("/*")) {
        if (logger.isTraceEnabled()) {
          logger.trace("Default mapping to {}", getHandlerDescription(handler));
        }
        setDefaultHandler(resolvedHandler);
      }
      else {
        PathPattern pathPattern = getPatternParser().parse(urlPath);
        duPutHandler(urlPath, resolvedHandler);
        doPutPathPattern(pathPattern, resolvedHandler);
        if (logger.isTraceEnabled()) {
          logger.trace("Mapped [{}] onto {}", urlPath, getHandlerDescription(handler));
        }
      }
    }
  }

  /**
   * Remove the mapping for the handler registered for the given URL path.
   *
   * @param urlPath the mapping to remove
   */
  public void unregisterHandler(String urlPath) {
    Assert.notNull(urlPath, "URL path is required");
    if (urlPath.equals("/")) {
      if (logger.isTraceEnabled()) {
        logger.trace("Removing root mapping: " + getRootHandler());
      }
      setRootHandler(null);
    }
    else if (urlPath.equals("/*")) {
      if (logger.isTraceEnabled()) {
        logger.trace("Removing default mapping: " + getDefaultHandler());
      }
      setDefaultHandler(null);
    }
    else {
      Object mappedHandler = this.handlerMap.get(urlPath);
      if (mappedHandler == null) {
        if (logger.isTraceEnabled()) {
          logger.trace("No mapping for [%s]".formatted(urlPath));
        }
      }
      else {
        if (logger.isTraceEnabled()) {
          logger.trace("Removing mapping \"{}\": {}", urlPath, getHandlerDescription(mappedHandler));
        }
        this.handlerMap.remove(urlPath);
        this.pathPatternHandlerMap.remove(getPatternParser().parse(urlPath));
      }
    }
  }

  protected void doPutPathPattern(PathPattern pathPattern, Object resolvedHandler) {
    pathPatternHandlerMap.put(pathPattern, resolvedHandler);
  }

  protected void duPutHandler(String urlPath, Object resolvedHandler) {
    handlerMap.put(urlPath, resolvedHandler);
  }

  private String getHandlerDescription(Object handler) {
    return handler instanceof String ? "'%s'".formatted(handler) : handler.toString();
  }

  /**
   * Return the handler mappings, with the registered path or pattern
   * as key and the handler object (or handler bean name in case of
   * a lazy-init handler), as value.
   *
   * @see #getDefaultHandler()
   */
  public final Map<String, Object> getHandlerMap() {
    return handlerMap;
  }

  /**
   * Identical to {@link #getHandlerMap()} but populated
   */
  public final Map<PathPattern, Object> getPathPatternHandlerMap() {
    return pathPatternHandlerMap;
  }

}
