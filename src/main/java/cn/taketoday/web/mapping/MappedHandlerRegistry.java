/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.web.mapping;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.AntPathMatcher;
import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 *         2019-12-24 15:46
 */
public class MappedHandlerRegistry extends AbstractHandlerRegistry {

    private PatternMapping[] patternMappings;
    private final Map<String, Object> handlers;

    private PathMatcher pathMatcher = new AntPathMatcher();

    public MappedHandlerRegistry() {
        this(new HashMap<>());
    }

    public MappedHandlerRegistry(int initialCapacity) {
        this(new HashMap<>(initialCapacity));
    }

    public MappedHandlerRegistry(Map<String, Object> handlers) {
        this.handlers = handlers;
    }

    public MappedHandlerRegistry(Map<String, Object> handlers, int order) {
        this.handlers = handlers;
        setOrder(order);
    }

    @Override
    protected Object lookupInternal(final RequestContext context) {
        final String handlerKey = computeKey(context);
        final Object handler = lookupHandler(handlerKey, context);
        if (handler == null) {
            return handlerNotFound(handlerKey, context);
        }
        return handler;
    }

    protected Object handlerNotFound(String handlerKey, RequestContext context) {
        return null;
    }

    protected String computeKey(final RequestContext context) {
        return context.requestURI();
    }

    protected Object lookupHandler(final String handlerKey, final RequestContext context) {
        final Object handler = getHandlers().get(handlerKey);
        if (handler != null) {
            return handler;
        }
        final PatternMapping[] patternMappings = getPatternMappings();
        if (patternMappings == null) {
            return null;
        }
        return matchingPatternHandler(handlerKey, patternMappings);
    }

    protected Object matchingPatternHandler(final String handlerKey, final PatternMapping[] patternMappings) {

        // pattern
        final Map<String, PatternMapping> matchedPatterns = new HashMap<>();
        final PathMatcher pathMatcher = getPathMatcher();

        for (final PatternMapping mapping : patternMappings) {
            final String pattern = mapping.getPattern();
            if (pathMatcher.match(pattern, handlerKey)) {
                matchedPatterns.put(pattern, mapping);
            }
        }

        if (matchedPatterns.isEmpty()) { // none matched
            return null;
        }

        final List<String> patterns = new ArrayList<>(matchedPatterns.keySet());
        patterns.sort(pathMatcher.getPatternComparator(handlerKey));

        if (log.isTraceEnabled() && matchedPatterns.size() > 1) {
            log.trace("Matching patterns {}", patterns);
        }
        return matchedPatterns.get(patterns.get(0)).getHandler();
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = nonNull(pathMatcher, "PathMatcher must not be null");
    }

    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    public void registerHandler(Object handler, String... handlerKeys) {
        for (final String path : nonNull(handlerKeys, "Handler Keys must not be null")) {
            registerHandler(path, handler);
        }
    }

    public void registerHandler(String handlerKey, Object handler) {
        nonNull(handler, "Handler must not be null");
        nonNull(handlerKey, "Handler Key must not be null");

        if (handler instanceof String) {
            handler = obtainApplicationContext().getBean((String) handler);
        }

        if (getPathMatcher().isPattern(handlerKey)) {
            addPatternMappings(new PatternMapping(handlerKey, handler));

            log.debug("Mapped Pattern [{}] onto [{}]", handlerKey, handler);
        }
        else {
            log.debug("Mapped [{}] onto [{}]", handlerKey, handler);
            handlers.put(handlerKey, handler); // TODO override handler
        }
    }

    public Map<String, Object> getHandlers() {
        return handlers;
    }

    public PatternMapping[] getPatternMappings() {
        return patternMappings;
    }

    public void addPatternMappings(final PatternMapping... mappings) {

        final PatternMapping[] patternMappings = getPatternMappings();
        final PatternMapping[] newArr = new PatternMapping[patternMappings.length + mappings.length];

        System.arraycopy(patternMappings, 0, newArr, 0, patternMappings.length);
        System.arraycopy(mappings, 0, newArr, patternMappings.length, mappings.length);

        setPatternMappings(OrderUtils.reversedSort(newArr));
    }

    public void setPatternMappings(PatternMapping... patternMappings) {
        this.patternMappings = patternMappings;
    }

}
