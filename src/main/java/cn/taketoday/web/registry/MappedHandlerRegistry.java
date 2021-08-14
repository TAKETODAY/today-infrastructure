/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.ConcurrentMapCache;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.Assert;
import cn.taketoday.core.EmptyObject;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.utils.CollectionUtils;
import cn.taketoday.core.utils.OrderUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.handler.PatternHandler;

/**
 * Map HandlerRegistry
 * <p>
 * 用字符串作Key
 * </p>
 *
 * @author TODAY <br>
 * 2019-12-24 15:46
 * @see #computeKey(RequestContext)
 */
public class MappedHandlerRegistry extends AbstractHandlerRegistry {
  /** @since 3.0 */
  static final String CACHE_NAME = MappedHandlerRegistry.class.getName() + "-pattern-matching";

  private final HashMap<String, Object> handlers = new HashMap<>();
  private List<PatternHandler> patternHandlers;

  private PathMatcher pathMatcher = new AntPathMatcher();
  private CompositeHandlerCustomizer handlerCustomizer;

  /** @since 3.0 */
  private Cache patternMatchingCache;

  /**
   * @since 3.0.1 init {@link #handlerCustomizer} delete initHandlerRegistry()
   */
  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext(context);
    List<HandlerCustomizer> customizers = context.getBeans(HandlerCustomizer.class);
    if (!CollectionUtils.isEmpty(customizers)) {
      this.handlerCustomizer = new CompositeHandlerCustomizer(customizers);
    }
  }

  @Override
  protected Object lookupInternal(final RequestContext context) {
    final String handlerKey = computeKey(context);
    final Object handler = lookupHandler(handlerKey, context);
    if (handler == null) {
      return handlerNotFound(handlerKey, context);
    }
    else {
      return handler;
    }
  }

  /**
   * Handle handler not found
   *
   * @param handlerKey
   *         Handler key
   * @param context
   *         Current request context
   *
   * @return A handler default is null
   */
  protected Object handlerNotFound(String handlerKey, RequestContext context) {
    return null;
  }

  /**
   * Compute a handler key from current request context
   *
   * @param context
   *         Current request context
   *
   * @return Handler key never be null
   */
  protected String computeKey(final RequestContext context) {
    return context.getRequestPath();
  }

  protected Object lookupHandler(final String handlerKey, final RequestContext context) {
    Object handler = handlers.get(handlerKey);
    if (handler == null) {
      final Cache patternMatchingCache = getPatternMatchingCache();
      handler = patternMatchingCache.get(handlerKey, false);
      if (handler == null) {
        handler = lookupPatternHandler(handlerKey, context);
        patternMatchingCache.put(handlerKey, handler);
      }
      else if (handler == EmptyObject.INSTANCE) {
        return null;
      }
    }
    return handler;
  }

  /**
   * Match pattern handler
   *
   * @param handlerKey
   *         Handler key
   * @param context
   *         current request context
   *
   * @return Matched pattern handler. If returns {@code null} indicates no handler
   */
  protected Object lookupPatternHandler(final String handlerKey, final RequestContext context) {
    final PatternHandler matched = matchingPatternHandler(handlerKey);
    if (matched == null) {
      return null;
    }
    else {
      return matched.getHandler();
    }
  }

  /** @since 3.0 */
  public final Cache getPatternMatchingCache() {
    Cache patternMatchingCache = this.patternMatchingCache;
    if (patternMatchingCache == null) {
      this.patternMatchingCache = patternMatchingCache = createPatternMatchingCache();
    }
    return patternMatchingCache;
  }

  /** @since 3.0 */
  protected ConcurrentMapCache createPatternMatchingCache() {
    return new ConcurrentMapCache(CACHE_NAME, 128);
  }

  /**
   * Set Pattern matching Cache
   *
   * @param patternMatchingCache
   *         a new Cache
   *
   * @since 3.0
   */
  public void setPatternMatchingCache(final Cache patternMatchingCache) {
    this.patternMatchingCache = patternMatchingCache;
  }

  protected PatternHandler matchingPatternHandler(final String handlerKey) {
    // pattern
    final HashMap<String, PatternHandler> matchedPatterns = new HashMap<>();
    final PathMatcher pathMatcher = getPathMatcher();
    if (!CollectionUtils.isEmpty(patternHandlers)) {
      for (final PatternHandler mapping : patternHandlers) {
        final String pattern = mapping.getPattern();
        if (matchingPattern(pathMatcher, pattern, handlerKey)) {
          matchedPatterns.put(pattern, mapping);
        }
      }
    }

    if (matchedPatterns.isEmpty()) { // none matched
      // match in map of handlers
      for (final Map.Entry<String, Object> entry : handlers.entrySet()) {
        final String pattern = entry.getKey();
        if (matchingPattern(pathMatcher, pattern, handlerKey)) {
          matchedPatterns.put(pattern, new PatternHandler(pattern, entry.getValue()));
        }
      }
      if (matchedPatterns.isEmpty()) {
        return null; // none matched
      }
    }

    final ArrayList<String> patterns = new ArrayList<>(matchedPatterns.keySet());
    patterns.sort(pathMatcher.getPatternComparator(handlerKey));

    if (log.isTraceEnabled() && matchedPatterns.size() > 1) {
      log.trace("Matching patterns {}", patterns);
    }
    return matchedPatterns.get(patterns.get(0));
  }

  protected boolean matchingPattern(
          final PathMatcher pathMatcher,
          final String pattern, final String handlerKey
  ) {
    return pathMatcher.match(pattern, handlerKey);
  }

  /**
   * Register a Handler to handler keys
   *
   * @param handler
   *         Target handler
   * @param handlerKeys
   *         Handler keys
   */
  public void registerHandler(final Object handler, final String... handlerKeys) {
    Assert.notNull(handlerKeys, "Handler Keys must not be null");

    for (final String path : handlerKeys) {
      registerHandler(path, handler);
    }
  }

  /**
   * Register a Handler to handler key
   *
   * @param handler
   *         Target handler
   * @param handlerKey
   *         Handler key
   */
  public void registerHandler(final String handlerKey, Object handler) {
    Assert.notNull(handler, "Handler must not be null");
    Assert.notNull(handlerKey, "Handler Key must not be null");

    if (handler instanceof String) {
      final String handlerName = (String) handler;
      final WebApplicationContext context = obtainApplicationContext();
      // singleton
      if (context.isSingleton(handlerName)) {
        handler = context.getBean(handlerName);
      }
    }
    handler = transformHandler(handlerKey, handler);
    // @since 3.0
    logMapping(handlerKey, handler);
    if (getPathMatcher().isPattern(handlerKey)) {
      addPatternHandlers(new PatternHandler(handlerKey, handler));
    }
    else {
      final Object oldHandler = handlers.put(handlerKey, handler);
      if (oldHandler != null && oldHandler != handler) {
        // @since 3.0
        logHandlerReplaced(handlerKey, oldHandler, handler);
      }
    }

    postRegisterHandler(handlerKey, handler);
  }

  protected void logHandlerReplaced(String handlerKey, Object oldHandler, Object newHandler) {
    log.warn("Refresh Handler Registration: {} onto {} old handler: {}",
             handlerKey, newHandler, oldHandler);
  }

  protected void logMapping(String handlerKey, Object handler) {
    log.info("Mapped '{}' onto {}", handlerKey, handler);
  }

  protected void postRegisterHandler(final String handlerKey, final Object handler) { }

  /**
   * Transform handler
   *
   * @param handlerKey
   *         handler key
   * @param handler
   *         Target handler
   *
   * @return Transformed handler
   */
  protected Object transformHandler(final String handlerKey, Object handler) {
    if (handlerCustomizer != null) {
      handler = handlerCustomizer.customize(handler);
    }
    return handler;
  }

  public void addPatternHandlers(final PatternHandler... handlers) {
    Assert.notNull(handlers, "Handlers must not be null");

    if (patternHandlers == null) {
      patternHandlers = new ArrayList<>(handlers.length);
    }
    Collections.addAll(patternHandlers, handlers);
    OrderUtils.reversedSort(patternHandlers);
  }

  public List<PatternHandler> getPatternHandlers() {
    return patternHandlers;
  }

  public void setPatternHandlers(List<PatternHandler> patternMappings) {
    this.patternHandlers = patternMappings;
  }

  /**
   * Setting a new {@link PathMatcher}
   *
   * @param pathMatcher
   *         new {@link PathMatcher}
   */
  public void setPathMatcher(PathMatcher pathMatcher) {
    Assert.notNull(pathMatcher, "PathMatcher must not be null");
    this.pathMatcher = pathMatcher;
  }

  /**
   * Get {@link PathMatcher}
   */
  public PathMatcher getPathMatcher() {
    return this.pathMatcher;
  }

  public void clearHandlers() {
    handlers.clear();
    setPatternHandlers(null);
  }

  public final Map<String, Object> getHandlers() {
    return handlers;
  }

  public void setHandlers(Map<String, Object> handlers) {
    this.handlers.putAll(handlers);
  }

}
