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
package cn.taketoday.web.cors;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.AntPathMatcher;
import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.RequestContext;

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

  private PathMatcher pathMatcher;
  private final Map<String, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();

  public UrlBasedCorsConfigurationSource() {
    this(new AntPathMatcher());
  }

  public UrlBasedCorsConfigurationSource(PathMatcher pathMatcher) {
    this.pathMatcher = pathMatcher;
  }

  /**
   * Set the PathMatcher implementation to use for matching URL paths
   * against registered URL patterns. Default is AntPathMatcher.
   */
  public void setPathMatcher(PathMatcher pathMatcher) {
    Assert.notNull(pathMatcher, "PathMatcher must not be null");
    this.pathMatcher = pathMatcher;
  }

  /**
   * Set CORS configuration based on URL patterns.
   */
  public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
    this.corsConfigurations.clear();
    if (corsConfigurations != null) {
      this.corsConfigurations.putAll(corsConfigurations);
    }
  }

  /**
   * Get the CORS configuration.
   */
  public Map<String, CorsConfiguration> getCorsConfigurations() {
    return this.corsConfigurations;
  }

  /**
   * Register a {@link CorsConfiguration} for the specified path pattern.
   */
  public void registerCorsConfiguration(String path, CorsConfiguration config) {
    this.corsConfigurations.put(path, config);
  }

  @Override
  public CorsConfiguration getCorsConfiguration(final RequestContext request) {
    final String lookupPath = request.getRequestPath();
    for (Map.Entry<String, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
      if (this.pathMatcher.match(entry.getKey(), lookupPath)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
