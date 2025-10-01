/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.util.pattern;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.http.server.PathContainer;
import infra.lang.Assert;
import infra.util.RouteMatcher;

/**
 * {@code RouteMatcher} built on {@link PathPatternParser} that uses
 * {@link PathContainer} and {@link PathPattern} as parsed representations of
 * routes and patterns.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PathPatternRouteMatcher implements RouteMatcher {

  private final PathPatternParser parser;

  private final ConcurrentHashMap<String, PathPattern> pathPatternCache = new ConcurrentHashMap<>();

  /**
   * Default constructor with {@link PathPatternParser} customized for
   * {@link infra.http.server.PathContainer.Options#MESSAGE_ROUTE MESSAGE_ROUTE}
   * and without matching of trailing separator.
   */
  public PathPatternRouteMatcher() {
    this.parser = new PathPatternParser();
    this.parser.setPathOptions(PathContainer.Options.MESSAGE_ROUTE);
    this.parser.setMatchOptionalTrailingSeparator(false);
  }

  /**
   * Constructor with given {@link PathPatternParser}.
   */
  public PathPatternRouteMatcher(PathPatternParser parser) {
    Assert.notNull(parser, "PathPatternParser is required");
    this.parser = parser;
  }

  @Override
  public Route parseRoute(String routeValue) {
    return new PathContainerRoute(PathContainer.parsePath(routeValue, this.parser.getPathOptions()));
  }

  @Override
  public boolean isPattern(String route) {
    return getPathPattern(route).hasPatternSyntax();
  }

  @Override
  public String combine(String pattern1, String pattern2) {
    return getPathPattern(pattern1).combine(getPathPattern(pattern2)).getPatternString();
  }

  @Override
  public boolean match(String pattern, Route route) {
    return getPathPattern(pattern).matches(getPathContainer(route));
  }

  @Override
  @Nullable
  public Map<String, String> matchAndExtract(String pattern, Route route) {
    PathMatchInfo info = getPathPattern(pattern).matchAndExtract(getPathContainer(route));
    return info != null ? info.getUriVariables() : null;
  }

  @Override
  public Comparator<String> getPatternComparator(Route route) {
    return Comparator.comparing(this::getPathPattern);
  }

  private PathPattern getPathPattern(String pattern) {
    return this.pathPatternCache.computeIfAbsent(pattern, this.parser::parse);
  }

  private PathContainer getPathContainer(Route route) {
    Assert.isInstanceOf(PathContainerRoute.class, route);
    return ((PathContainerRoute) route).pathContainer;
  }

  private record PathContainerRoute(PathContainer pathContainer) implements Route {

    @Override
    public String value() {
      return this.pathContainer.value();
    }

    @Override
    public String toString() {
      return value();
    }
  }

}
