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

package cn.taketoday.web.handler.mvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebContentGenerator;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Handler interceptor that checks the request for supported methods and a
 * required session and prepares the response by applying the configured
 * cache settings.
 *
 * <p>Cache settings may be configured for specific URLs via path pattern with
 * {@link #addCacheMapping(CacheControl, String...)} and
 * {@link #setCacheMappings(Properties)}, along with a fallback on default
 * settings for all URLs via {@link #setCacheControl(CacheControl)}.
 *
 * <p>Pattern matching can be done with {@link PathMatcher} or with parsed
 * {@link PathPattern}s. The syntax is largely the same with the latter being
 * more tailored for web usage and more efficient.
 *
 * <p>All the settings supported by this interceptor can also be set on
 * {@link AbstractController}. This interceptor is mainly intended for applying
 * checks and preparations to a set of controllers mapped by a HandlerMapping.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PathMatcher
 * @see AntPathMatcher
 * @since 4.0 2022/2/8 15:46
 */
public class WebContentInterceptor extends WebContentGenerator implements HandlerInterceptor {

  private final PathPatternParser patternParser;

  private final Map<PathPattern, Integer> cacheMappings = new HashMap<>();

  private final Map<PathPattern, CacheControl> cacheControlMappings = new HashMap<>();

  /**
   * Default constructor with {@link PathPatternParser#defaultInstance}.
   */
  public WebContentInterceptor() {
    this(PathPatternParser.defaultInstance);
  }

  /**
   * Constructor with a {@link PathPatternParser} to parse patterns with.
   */
  public WebContentInterceptor(PathPatternParser parser) {
    // No restriction of HTTP methods by default,
    // in particular for use with annotated controllers...
    super(false);
    this.patternParser = parser;
  }

  /**
   * Map settings for  cache seconds to specific URL paths via patterns.
   * <p>Overrides the default cache seconds setting of this interceptor.
   * Can specify "-1" to exclude a URL path from default caching.
   * <p>For pattern syntax see {@link AntPathMatcher} and {@link PathPattern}
   * as well as the class-level Javadoc for details for when each is used.
   * The syntax is largely the same with {@link PathPattern} more tailored for
   * web usage.
   * <p><b>NOTE:</b> Path patterns are not supposed to overlap. If a request
   * matches several mappings, it is effectively undefined which one will apply
   * (due to the lack of key ordering in {@code java.util.Properties}).
   *
   * @param cacheMappings a mapping between URL paths (as keys) and
   * cache seconds (as values, need to be integer-parsable)
   * @see #setCacheSeconds
   */
  public void setCacheMappings(Properties cacheMappings) {
    this.cacheMappings.clear();
    Set<String> propNames = cacheMappings.stringPropertyNames();
    for (String path : propNames) {
      int cacheSeconds = Integer.parseInt(cacheMappings.getProperty(path));
      this.cacheMappings.put(patternParser.parse(path), cacheSeconds);
    }
  }

  /**
   * Map specific URL paths to a specific {@link cn.taketoday.http.CacheControl}.
   * <p>Overrides the default cache seconds setting of this interceptor.
   * Can specify a empty {@link cn.taketoday.http.CacheControl} instance
   * to exclude a URL path from default caching.
   * <p>For pattern syntax see {@link AntPathMatcher} and {@link PathPattern}
   * as well as the class-level Javadoc for details for when each is used.
   * The syntax is largely the same with {@link PathPattern} more tailored for
   * web usage.
   * <p><b>NOTE:</b> Path patterns are not supposed to overlap. If a request
   * matches several mappings, it is effectively undefined which one will apply
   * (due to the lack of key ordering in the underlying {@code java.util.HashMap}).
   *
   * @param cacheControl the {@code CacheControl} to use
   * @param paths the URL paths that will map to the given {@code CacheControl}
   * @see #setCacheSeconds
   */
  public void addCacheMapping(CacheControl cacheControl, String... paths) {
    for (String path : paths) {
      cacheControlMappings.put(patternParser.parse(path), cacheControl);
    }
  }

  @Override
  public boolean beforeProcess(RequestContext request, Object handler) {
    checkRequest(request);
    RequestPath path = request.getLookupPath();

    if (CollectionUtils.isNotEmpty(cacheControlMappings)) {
      CacheControl control = lookupCacheControl(path);
      if (control != null) {
        if (log.isTraceEnabled()) {
          log.trace("Applying {}", control);
        }
        applyCacheControl(request, control);
        return true;
      }
    }

    if (CollectionUtils.isNotEmpty(cacheMappings)) {
      Integer cacheSeconds = lookupCacheSeconds(path);
      if (cacheSeconds != null) {
        if (log.isTraceEnabled()) {
          log.trace("Applying cacheSeconds {}", cacheSeconds);
        }
        applyCacheSeconds(request, cacheSeconds);
        return true;
      }
    }

    prepareResponse(request);
    return true;
  }

  /**
   * Find a {@link cn.taketoday.http.CacheControl} instance for the
   * given parsed {@link PathContainer path}. This is used when the
   * {@code HandlerMapping} uses parsed {@code PathPatterns}.
   *
   * @param path the path to match to
   * @return the matched {@code CacheControl}, or {@code null} if no match
   */
  @Nullable
  protected CacheControl lookupCacheControl(PathContainer path) {
    for (Map.Entry<PathPattern, CacheControl> entry : cacheControlMappings.entrySet()) {
      if (entry.getKey().matches(path)) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Find a cacheSeconds value for the given parsed {@link PathContainer path}.
   * This is used when the {@code HandlerMapping} uses parsed {@code PathPatterns}.
   *
   * @param path the path to match to
   * @return the matched cacheSeconds, or {@code null} if there is no match
   */
  @Nullable
  protected Integer lookupCacheSeconds(PathContainer path) {
    for (Map.Entry<PathPattern, Integer> entry : cacheMappings.entrySet()) {
      if (entry.getKey().matches(path)) {
        return entry.getValue();
      }
    }
    return null;
  }

}
