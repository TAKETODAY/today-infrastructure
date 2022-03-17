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

package cn.taketoday.util;

import java.util.Comparator;
import java.util.Map;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@code RouteMatcher} that delegates to a {@link PathMatcher}.
 *
 * <p><strong>Note:</strong> This implementation is not efficient since
 * {@code PathMatcher} treats paths and patterns as Strings. For more optimized
 * performance use the {@code PathPatternRouteMatcher} from {@code today-web}
 * which enables use of parsed routes and patterns.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/6 20:49
 * @since 4.0
 */
public class SimpleRouteMatcher implements RouteMatcher {

  private final PathMatcher pathMatcher;

  /**
   * Create a new {@code SimpleRouteMatcher} for the given
   * {@link PathMatcher} delegate.
   */
  public SimpleRouteMatcher(PathMatcher pathMatcher) {
    Assert.notNull(pathMatcher, "PathMatcher is required");
    this.pathMatcher = pathMatcher;
  }

  /**
   * Return the underlying {@link PathMatcher} delegate.
   */
  public PathMatcher getPathMatcher() {
    return this.pathMatcher;
  }

  @Override
  public Route parseRoute(String route) {
    return new DefaultRoute(route);
  }

  @Override
  public boolean isPattern(String route) {
    return this.pathMatcher.isPattern(route);
  }

  @Override
  public String combine(String pattern1, String pattern2) {
    return this.pathMatcher.combine(pattern1, pattern2);
  }

  @Override
  public boolean match(String pattern, Route route) {
    return this.pathMatcher.match(pattern, route.value());
  }

  @Override
  @Nullable
  public Map<String, String> matchAndExtract(String pattern, Route route) {
    if (!match(pattern, route)) {
      return null;
    }
    return this.pathMatcher.extractUriTemplateVariables(pattern, route.value());
  }

  @Override
  public Comparator<String> getPatternComparator(Route route) {
    return this.pathMatcher.getPatternComparator(route.value());
  }

  private record DefaultRoute(String path) implements Route {

    @Override
    public String value() {
      return this.path;
    }

    @Override
    public String toString() {
      return value();
    }
  }

}
