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

package cn.taketoday.web.handler;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;
import cn.taketoday.web.util.pattern.PatternParseException;

/**
 * Wraps a {@link HandlerInterceptor} and uses URL patterns to determine whether
 * it applies to a given request.
 *
 * <p>Pattern matching can be done with {@link PathMatcher} or with parsed
 * {@link PathPattern}. The syntax is largely the same with the latter being more
 * tailored for web usage and more efficient.
 *
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/18 15:36
 */
public final class MappedInterceptor implements HandlerInterceptor {
  private static final PathMatcher defaultPathMatcher = new AntPathMatcher();

  @Nullable
  private final CompiledPattern[] includePatterns;

  @Nullable
  private final CompiledPattern[] excludePatterns;

  private final HandlerInterceptor interceptor;

  private PathMatcher pathMatcher = defaultPathMatcher;

  /**
   * Create an instance with the given include and exclude patterns along with
   * the target interceptor for the mappings.
   *
   * @param includePatterns patterns to which requests must match, or null to
   * match all paths
   * @param excludePatterns patterns to which requests must not match
   * @param interceptor the target interceptor
   * @param parser a parser to use to pre-parse patterns into {@link PathPattern};
   * when not provided, {@link PathPatternParser#defaultInstance} is used.
   */
  public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
          HandlerInterceptor interceptor, @Nullable PathPatternParser parser) {

    this.includePatterns = CompiledPattern.initPatterns(includePatterns, parser);
    this.excludePatterns = CompiledPattern.initPatterns(excludePatterns, parser);
    this.interceptor = interceptor;
  }

  /**
   * Variant of
   * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
   * with include patterns only.
   */
  public MappedInterceptor(@Nullable String[] includePatterns, HandlerInterceptor interceptor) {
    this(includePatterns, null, interceptor);
  }

  /**
   * Variant of
   * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
   * without a provided parser.
   */
  public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
          HandlerInterceptor interceptor) {

    this(includePatterns, excludePatterns, interceptor, null);
  }

  /**
   * Configure the PathMatcher to use to match URL paths with against include
   * and exclude patterns.
   * <p>This is an advanced property that should be used only when a
   * customized {@link AntPathMatcher} or a custom PathMatcher is required.
   * <p>By default this is {@link AntPathMatcher}.
   */
  public void setPathMatcher(PathMatcher pathMatcher) {
    Assert.notNull(pathMatcher, "pathMatcher is required");
    this.pathMatcher = pathMatcher;
  }

  /**
   * The {@link #setPathMatcher(PathMatcher) configured} PathMatcher.
   */
  public PathMatcher getPathMatcher() {
    return this.pathMatcher;
  }

  /**
   * Return the patterns this interceptor is mapped to.
   */
  @Nullable
  public String[] getPathPatterns() {
    if (ObjectUtils.isNotEmpty(includePatterns)) {
      int i = 0;
      String[] patterns = new String[includePatterns.length];
      for (CompiledPattern includePattern : includePatterns) {
        patterns[i++] = includePattern.patternString;
      }
      return patterns;
    }
    return null;
  }

  /**
   * The target {@link HandlerInterceptor} to invoke in case of a match.
   */
  public HandlerInterceptor getInterceptor() {
    return this.interceptor;
  }

  /**
   * Check whether this interceptor is mapped to the request.
   * <p>The request mapping path is expected to have been resolved externally.
   * See also class-level Javadoc.
   *
   * @param request the request to match to
   * @return {@code true} if the interceptor should be applied to the request
   */
  public boolean matches(RequestContext request) {
    RequestPath lookupPath = request.getLookupPath();
    return matches(lookupPath);
  }

  /**
   * Check whether this interceptor is mapped to the request.
   * <p>The request mapping path is expected to have been resolved externally.
   * See also class-level Javadoc.
   *
   * @param lookupPath the request path to match to
   * @return {@code true} if the interceptor should be applied to the request
   */
  public boolean matches(RequestPath lookupPath) {
    if (ObjectUtils.isNotEmpty(excludePatterns)) {
      for (CompiledPattern adapter : excludePatterns) {
        if (doMatch(adapter, lookupPath)) {
          return false;
        }
      }
    }
    if (ObjectUtils.isEmpty(includePatterns)) {
      return true;
    }
    for (CompiledPattern adapter : includePatterns) {
      if (doMatch(adapter, lookupPath)) {
        return true;
      }
    }
    return false;
  }

  private boolean doMatch(CompiledPattern adapter, PathContainer path) {
    PathPattern pathPattern = adapter.pathPattern;
    if (pathPattern != null) {
      return pathPattern.matches(path);
    }

    String lookupPath = WebUtils.removeSemicolonContent(path.value());
    return pathMatcher.match(adapter.patternString, lookupPath);
  }

  // HandlerInterceptor delegation

  @Override
  public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
    return interceptor.beforeProcess(request, handler);
  }

  @Override
  public void afterProcess(RequestContext request, Object handler, Object result) throws Throwable {
    interceptor.afterProcess(request, handler, result);
  }

  @Override
  public Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
    return interceptor.intercept(request, chain);
  }

  /**
   * Contains both the parsed {@link PathPattern} and the raw String pattern,
   * and uses the former when the cached path is {@link PathContainer} or the
   * latter otherwise. If the pattern cannot be parsed due to unsupported
   * syntax, then {@link PathMatcher} is used for all requests.
   */
  private final static class CompiledPattern {

    public final String patternString;

    @Nullable
    public final PathPattern pathPattern;

    public CompiledPattern(String pattern, @Nullable PathPatternParser parser) {
      this.patternString = pattern;
      this.pathPattern = initPathPattern(pattern, parser);
    }

    @Nullable
    private static PathPattern initPathPattern(String pattern, @Nullable PathPatternParser parser) {
      if (parser == null) {
        parser = PathPatternParser.defaultInstance;
      }
      try {
        return parser.parse(pattern);
      }
      catch (PatternParseException ex) {
        return null;
      }
    }

    @Nullable
    public static CompiledPattern[] initPatterns(
            @Nullable String[] patterns, @Nullable PathPatternParser parser) {
      if (ObjectUtils.isEmpty(patterns)) {
        return null;
      }

      int i = 0;
      CompiledPattern[] result = new CompiledPattern[patterns.length];
      for (String pattern : patterns) {
        result[i++] = new CompiledPattern(pattern, parser);
      }
      return result;
    }

  }

}
