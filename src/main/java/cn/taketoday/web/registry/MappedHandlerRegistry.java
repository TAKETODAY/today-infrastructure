/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import cn.taketoday.context.AntPathMatcher;
import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.PatternHandler;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

/**
 * @author TODAY <br>
 *         2019-12-24 15:46
 */
public class MappedHandlerRegistry extends AbstractHandlerRegistry {

  private final Map<String, Object> handlers;
  private List<PatternHandler> patternHandlers;

  private PathMatcher pathMatcher = new AntPathMatcher();

  public MappedHandlerRegistry() {
    this(new HashMap<>());
  }

  public MappedHandlerRegistry(int initialCapacity) {
    this(new HashMap<>(initialCapacity));
  }

  public MappedHandlerRegistry(Map<String, Object> handlers) {
    this(handlers, LOWEST_PRECEDENCE);
  }

  public MappedHandlerRegistry(Map<String, Object> handlers, int order) {
    this.handlers = nonNull(handlers, "Handlers mappings can not be null");
    setOrder(order);
  }

  @Override
  protected Object lookupInternal(final RequestContext context) {
    final String handlerKey = computeKey(context);
    final Object handler = lookupHandler(handlerKey);
    return handler == null ? handlerNotFound(handlerKey, context) : handler;
  }

  /**
   * Handle handler not found
   *
   * @param handlerKey
   *            Handler key
   * @param context
   *            Current request context
   * @return A handler default is null
   */
  protected Object handlerNotFound(String handlerKey, RequestContext context) {
    return null;
  }

  /**
   * Compute a handler key from current request context
   *
   * @param context
   *            Current request context
   * @return Handler key never be null
   */
  protected String computeKey(final RequestContext context) {
    return context.requestURI();
  }

  protected Object lookupHandler(final String handlerKey) {
    final Object handler = handlers.get(handlerKey);
    return handler == null ? lookupPatternHandler(handlerKey) : handler;
  }

  /**
   * Match pattern handler
   *
   * @param handlerKey
   *            Handler key
   * @return Matched pattern handler. If returns {@code null} indicates no handler
   */
  protected Object lookupPatternHandler(final String handlerKey) {
    final PatternHandler matched = matchingPatternHandler(handlerKey);
    return matched == null ? null : matched.getHandler();
  }

  protected PatternHandler matchingPatternHandler(final String handlerKey) {
    final List<PatternHandler> patternHandlers = getPatternHandlers();
    if (CollectionUtils.isEmpty(patternHandlers)) {
      return null;
    }
    // pattern
    final HashMap<String, PatternHandler> matchedPatterns = new HashMap<>();
    final PathMatcher pathMatcher = getPathMatcher();

    for (final PatternHandler mapping : patternHandlers) {
      final String pattern = mapping.getPattern();
      if (pathMatcher.match(pattern, handlerKey)) {
        matchedPatterns.put(pattern, mapping);
      }
    }

    if (matchedPatterns.isEmpty()) { // none matched
      return null;
    }

    final ArrayList<String> patterns = new ArrayList<>(matchedPatterns.keySet());
    patterns.sort(pathMatcher.getPatternComparator(handlerKey));

    if (log.isTraceEnabled() && matchedPatterns.size() > 1) {
      log.trace("Matching patterns {}", patterns);
    }
    return matchedPatterns.get(patterns.get(0));
  }

  /**
   * Register a Handler to handler keys
   *
   * @param handler
   *            Target handler
   * @param handlerKeys
   *            Handler keys
   */
  public void registerHandler(Object handler, String... handlerKeys) {
    for (final String path : nonNull(handlerKeys, "Handler Keys must not be null")) {
      registerHandler(path, handler);
    }
  }

  /**
   *
   * Register a Handler to handler key
   *
   * @param handler
   *            Target handler
   * @param handlerKey
   *            Handler key
   */
  public void registerHandler(String handlerKey, Object handler) {
    nonNull(handler, "Handler must not be null");
    nonNull(handlerKey, "Handler Key must not be null");

    if (handler instanceof String) {
      handler = obtainApplicationContext().getBean((String) handler);
    }

    log.debug("Mapped [{}] onto [{}]", handlerKey, handler);

    if (getPathMatcher().isPattern(handlerKey)) {
      addPatternHandlers(new PatternHandler(handlerKey, handler));
    }
    else {
      handlers.put(handlerKey, handler); // TODO override handler
    }
  }

  public void addPatternHandlers(final PatternHandler... handlers) {// TODO
    nonNull(handlers, "Handlers must not be null");

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
   *            new {@link PathMatcher}
   */
  public void setPathMatcher(PathMatcher pathMatcher) {
    this.pathMatcher = nonNull(pathMatcher, "PathMatcher must not be null");
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

}
