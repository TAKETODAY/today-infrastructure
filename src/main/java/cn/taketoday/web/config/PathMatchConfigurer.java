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

package cn.taketoday.web.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.util.UrlPathHelper;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Configure path matching options. The options are applied to the following:
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 20:41
 */
public class PathMatchConfigurer {

  @Nullable
  private PathPatternParser patternParser;

  @Nullable
  private Boolean trailingSlashMatch;

  @Nullable
  private Map<String, Predicate<Class<?>>> pathPrefixes;

  @Nullable
  private UrlPathHelper urlPathHelper;

  @Nullable
  private PathMatcher pathMatcher;

  @Nullable
  private PathPatternParser defaultPatternParser;

  @Nullable
  private UrlPathHelper defaultUrlPathHelper;

  @Nullable
  private PathMatcher defaultPathMatcher;

  /**
   * Enable use of parsed {@link PathPattern}s as described in
   * {@link AbstractHandlerMapping#setPatternParser(PathPatternParser)}.
   * <p><strong>Note:</strong> This is mutually exclusive with use of
   * {@link #setUrlPathHelper(UrlPathHelper)} and
   * {@link #setPathMatcher(PathMatcher)}.
   * <p>By default this is not enabled.
   *
   * @param patternParser the parser to pre-parse patterns with
   */
  public PathMatchConfigurer setPatternParser(PathPatternParser patternParser) {
    this.patternParser = patternParser;
    return this;
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   * If enabled a method mapped to "/users" also matches to "/users/".
   * <p>The default value is {@code true}.
   */
  public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
    this.trailingSlashMatch = trailingSlashMatch;
    return this;
  }

  /**
   * Configure a path prefix to apply to matching controller methods.
   * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
   * method whose controller type is matched by the corresponding
   * {@code Predicate}. The prefix for the first matching predicate is used.
   * <p>Consider using {@link org.springframework.web.method.HandlerTypePredicate
   * HandlerTypePredicate} to group controllers.
   *
   * @param prefix the prefix to apply
   * @param predicate a predicate for matching controller types
   */
  public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
    if (this.pathPrefixes == null) {
      this.pathPrefixes = new LinkedHashMap<>();
    }
    this.pathPrefixes.put(prefix, predicate);
    return this;
  }

  /**
   * Set the UrlPathHelper to use to resolve the mapping path for the application.
   * <p><strong>Note:</strong> This property is mutually exclusive with and
   * ignored when {@link #setPatternParser(PathPatternParser)} is set.
   */
  public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
    this.urlPathHelper = urlPathHelper;
    return this;
  }

  /**
   * Set the PathMatcher to use for String pattern matching.
   * <p>By default this is {@link AntPathMatcher}.
   * <p><strong>Note:</strong> This property is mutually exclusive with and
   * ignored when {@link #setPatternParser(PathPatternParser)} is set.
   */
  public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
    this.pathMatcher = pathMatcher;
    return this;
  }

  /**
   * Return the {@link PathPatternParser} to use, if configured.
   */
  @Nullable
  public PathPatternParser getPatternParser() {
    return this.patternParser;
  }

  @Nullable
  @Deprecated
  public Boolean isUseTrailingSlashMatch() {
    return this.trailingSlashMatch;
  }

  @Nullable
  protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
    return this.pathPrefixes;
  }

  @Nullable
  public UrlPathHelper getUrlPathHelper() {
    return this.urlPathHelper;
  }

  @Nullable
  public PathMatcher getPathMatcher() {
    return this.pathMatcher;
  }

  /**
   * Return the configured UrlPathHelper or a default, shared instance otherwise.
   */
  protected UrlPathHelper getUrlPathHelperOrDefault() {
    if (this.urlPathHelper != null) {
      return this.urlPathHelper;
    }
    if (this.defaultUrlPathHelper == null) {
      this.defaultUrlPathHelper = new UrlPathHelper();
    }
    return this.defaultUrlPathHelper;
  }

  /**
   * Return the configured PathMatcher or a default, shared instance otherwise.
   */
  protected PathMatcher getPathMatcherOrDefault() {
    if (this.pathMatcher != null) {
      return this.pathMatcher;
    }
    if (this.defaultPathMatcher == null) {
      this.defaultPathMatcher = new AntPathMatcher();
    }
    return this.defaultPathMatcher;
  }

  /**
   * Return the configured PathPatternParser or a default, shared instance otherwise.
   */
  public PathPatternParser getPatternParserOrDefault() {
    if (this.patternParser != null) {
      return this.patternParser;
    }
    if (this.defaultPatternParser == null) {
      this.defaultPatternParser = new PathPatternParser();
    }
    return this.defaultPatternParser;
  }
}
