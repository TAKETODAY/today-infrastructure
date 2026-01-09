/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.cors;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.http.server.PathContainer;
import infra.lang.Assert;
import infra.util.AntPathMatcher;
import infra.web.RequestContext;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

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
  public @Nullable CorsConfiguration getCorsConfiguration(final RequestContext request) {
    PathContainer lookupPath = request.getRequestPath();
    for (Map.Entry<PathPattern, CorsConfiguration> entry : corsConfigurations.entrySet()) {
      if (entry.getKey().matches(lookupPath)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
