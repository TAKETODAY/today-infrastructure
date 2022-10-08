/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.PatternHandler;

/**
 * Map HandlerMapping
 * <p>
 * 用字符串作Key
 * </p>
 *
 * @author TODAY <br>
 * 2019-12-24 15:46
 */
public class MappedHandlerMapping extends AbstractUrlHandlerMapping {
  /** @since 3.0 */
  static final String CACHE_NAME = MappedHandlerMapping.class.getName() + "-pattern-matching";

  private final HashMap<String, Object> handlers = new HashMap<>();

  @Nullable
  protected List<PatternHandler> patternHandlers;

  private PathMatcher pathMatcher = new AntPathMatcher();

  /** @since 3.0 */
  private Cache patternMatchingCache;

  @Override
  protected Object getHandlerInternal(RequestContext context) {
    String handlerKey = computeKey(context);
    Object handler = lookupHandler(handlerKey, context);
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
   * @param handlerKey Handler key
   * @param context Current request context
   * @return A handler default is null
   */
  protected Object handlerNotFound(String handlerKey, RequestContext context) {
    return null;
  }

  /**
   * Compute a handler key from current request context
   *
   * @param context Current request context
   * @return Handler key never be null
   */
  protected String computeKey(RequestContext context) {
    return context.getRequestURI();
  }

  protected Object lookupHandler(String handlerKey, RequestContext context) {
    Object handler = handlers.get(handlerKey);
    if (handler == null) {
      Cache matchingCache = getPatternMatchingCache();
      Cache.ValueWrapper wrapper = matchingCache.get(handlerKey);
      if (wrapper == null) {
        // null -> NullValue.INSTANCE
        handler = lookupPatternHandler(handlerKey, context);
        matchingCache.put(handlerKey, handler);
      }
      else {
        handler = wrapper.get();
      }
    }
    return handler;
  }

  /**
   * Match pattern handler
   *
   * @param handlerKey Handler key
   * @param context current request context
   * @return Matched pattern handler. If returns {@code null} indicates no handler
   */
  @Nullable
  protected Object lookupPatternHandler(String handlerKey, RequestContext context) {
    PatternHandler matched = matchingPatternHandler(handlerKey);
    if (matched == null) {
      return null;
    }
    else {
      return matched.getHandler();
    }
  }

  /** @since 3.0 */
  public final Cache getPatternMatchingCache() {
    Cache matchingCache = this.patternMatchingCache;
    if (matchingCache == null) {
      matchingCache = createPatternMatchingCache();
      this.patternMatchingCache = matchingCache;
    }
    return matchingCache;
  }

  /** @since 3.0 */
  @NonNull
  protected ConcurrentMapCache createPatternMatchingCache() {
    return new ConcurrentMapCache(CACHE_NAME, true);
  }

  /**
   * Set Pattern matching Cache
   *
   * @param patternMatchingCache a new Cache
   * @since 3.0
   */
  public void setPatternMatchingCache(Cache patternMatchingCache) {
    this.patternMatchingCache = patternMatchingCache;
  }

  @Nullable
  protected PatternHandler matchingPatternHandler(String handlerKey) {
    // pattern
    HashMap<String, PatternHandler> matchedPatterns = new HashMap<>();
    PathMatcher pathMatcher = getPathMatcher();
    if (CollectionUtils.isNotEmpty(patternHandlers)) {
      for (PatternHandler mapping : patternHandlers) {
        String pattern = mapping.getPattern();
        if (matchingPattern(pathMatcher, pattern, handlerKey)) {
          matchedPatterns.put(pattern, mapping);
        }
      }
    }

    if (matchedPatterns.isEmpty()) { // none matched
      // match pattern in map of handlers
      for (Map.Entry<String, Object> entry : handlers.entrySet()) {
        String pattern = entry.getKey();
        if (matchingPattern(pathMatcher, pattern, handlerKey)) {
          matchedPatterns.put(pattern, new PatternHandler(pattern, entry.getValue()));
        }
      }
      if (matchedPatterns.isEmpty()) {
        return null; // none matched
      }
    }

    ArrayList<String> patterns = new ArrayList<>(matchedPatterns.keySet());
    patterns.sort(pathMatcher.getPatternComparator(handlerKey));

    if (matchedPatterns.size() > 1 && log.isTraceEnabled()) {
      log.trace("Matching patterns {}", patterns);
    }
    return matchedPatterns.get(patterns.get(0));
  }

  protected boolean matchingPattern(
          PathMatcher pathMatcher,
          String pattern, String handlerKey
  ) {
    return pathMatcher.match(pattern, handlerKey);
  }

  /**
   * Register a Handler to handler keys
   *
   * @param handler Target handler
   * @param handlerKeys Handler keys
   */
  public void registerHandler(Object handler, String... handlerKeys) {
    Assert.notNull(handlerKeys, "Handler Keys must not be null");

    for (String path : handlerKeys) {
      registerHandler(path, handler);
    }
  }

  /**
   * Register a Handler to handler key
   *
   * @param handler Target handler
   * @param handlerKey Handler key
   */
  public void registerHandler(String handlerKey, Object handler) {
    Assert.notNull(handler, "Handler must not be null");
    Assert.notNull(handlerKey, "Handler Key must not be null");

    if (handler instanceof String handlerName) {
      ApplicationContext context = obtainApplicationContext();
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
      Object oldHandler = handlers.put(handlerKey, handler);
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

  protected void postRegisterHandler(String handlerKey, Object handler) { }

  /**
   * Transform handler
   *
   * @param handlerKey handler key
   * @param handler Target handler
   * @return Transformed handler
   */
  protected Object transformHandler(String handlerKey, Object handler) {
    return handler;
  }

  public void addPatternHandlers(PatternHandler... handlers) {
    Assert.notNull(handlers, "Handlers must not be null");
    List<PatternHandler> patternHandlers = getPatternHandlers();
    if (patternHandlers == null) {
      patternHandlers = new ArrayList<>(handlers.length);
      this.patternHandlers = patternHandlers;
    }
    CollectionUtils.addAll(patternHandlers, handlers);
    sort(patternHandlers);
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings("all")
  protected void sort(List list) {
    AnnotationAwareOrderComparator.sort(list);
  }

  @Nullable
  public List<PatternHandler> getPatternHandlers() {
    return patternHandlers;
  }

  public void setPatternHandlers(@Nullable List<PatternHandler> patternMappings) {
    this.patternHandlers = patternMappings;
  }

  /**
   * Setting a new {@link PathMatcher}
   *
   * @param pathMatcher new {@link PathMatcher}
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

  public void setHandlers(@Nullable Map<String, Object> handlers) {
    CollectionUtils.putAll(this.handlers, handlers);
  }

}
