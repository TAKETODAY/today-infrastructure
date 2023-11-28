/*
 * Copyright 2017 - 2023 the original author or authors.
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
package cn.taketoday.web.cors;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Provide a per request {@link CorsConfiguration} instance based on a
 * collection of {@link CorsConfiguration} mapped on path patterns.
 *
 * <p>Exact path mapping URIs (such as {@code "/admin"}) are supported
 * as well as Ant-style path patterns (such as {@code "/admin/**"}).
 *
 * @author Sebastien Deleuze
 * @author TODAY 2020/12/10 23:09
 * @since 3.0
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {

  private final Map<PathPattern, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();

  private final PathPatternParser patternParser;

  /**
   * Default constructor with {@link PathPatternParser#defaultInstance}.
   */
  public UrlBasedCorsConfigurationSource() {
    this(PathPatternParser.defaultInstance);
  }

  /**
   * Constructor with a {@link PathPatternParser} to parse patterns with.
   *
   * @param parser the parser to use
   * @since 4.0
   */
  public UrlBasedCorsConfigurationSource(PathPatternParser parser) {
    Assert.notNull(parser, "PathPatternParser is required");
    this.patternParser = parser;
  }

  /**
   * Set the CORS configuration mappings.
   * <p>For pattern syntax see {@link AntPathMatcher} and {@link PathPattern}
   * as well as class-level Javadoc for details on which one may in use.
   * Generally the syntax is largely the same with {@link PathPattern} more
   * tailored for web usage.
   *
   * @param corsConfigurations the mappings to use
   * @see PathPattern
   * @see AntPathMatcher
   */
  public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> corsConfigurations) {
    this.corsConfigurations.clear();
    if (corsConfigurations != null) {
      for (Map.Entry<String, CorsConfiguration> entry : corsConfigurations.entrySet()) {
        registerCorsConfiguration(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Variant of {@link #setCorsConfigurations(Map)} to register one mapping at a time.
   *
   * @param pattern the mapping pattern
   * @param config the CORS configuration to use for the pattern
   * @see PathPattern
   * @see AntPathMatcher
   */
  public void registerCorsConfiguration(String pattern, CorsConfiguration config) {
    this.corsConfigurations.put(patternParser.parse(pattern), config);
  }

  /**
   * Get the CORS configuration.
   */
  public Map<PathPattern, CorsConfiguration> getCorsConfigurations() {
    return this.corsConfigurations;
  }

  @Override
  public CorsConfiguration getCorsConfiguration(final RequestContext request) {
    PathContainer lookupPath = request.getLookupPath();
    for (Map.Entry<PathPattern, CorsConfiguration> entry : corsConfigurations.entrySet()) {
      if (entry.getKey().matches(lookupPath)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
