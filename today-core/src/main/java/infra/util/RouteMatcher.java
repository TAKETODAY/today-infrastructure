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

package infra.util;

import java.util.Comparator;
import java.util.Map;

import infra.lang.Nullable;

/**
 * Contract for matching routes to patterns.
 *
 * <p>Equivalent to {@link PathMatcher}, but enables use of parsed representations
 * of routes and patterns for efficiency reasons in scenarios where routes from
 * incoming messages are continuously matched against a large number of message
 * handler patterns.
 *
 * @author Rossen Stoyanchev
 * @see PathMatcher
 * @since 4.0
 */
public interface RouteMatcher {

  /**
   * Return a parsed representation of the given route.
   *
   * @param routeValue the route to parse
   * @return the parsed representation of the route
   */
  Route parseRoute(String routeValue);

  /**
   * Whether the given {@code route} contains pattern syntax which requires
   * the {@link #match(String, Route)} method, or if it is a regular String
   * that could be compared directly to others.
   *
   * @param route the route to check
   * @return {@code true} if the given {@code route} represents a pattern
   */
  boolean isPattern(String route);

  /**
   * Combines two patterns into a single pattern.
   *
   * @param pattern1 the first pattern
   * @param pattern2 the second pattern
   * @return the combination of the two patterns
   * @throws IllegalArgumentException when the two patterns cannot be combined
   */
  String combine(String pattern1, String pattern2);

  /**
   * Match the given route against the given pattern.
   *
   * @param pattern the pattern to try to match
   * @param route the route to test against
   * @return {@code true} if there is a match, {@code false} otherwise
   */
  boolean match(String pattern, Route route);

  /**
   * Match the pattern to the route and extract template variables.
   *
   * @param pattern the pattern, possibly containing templates variables
   * @param route the route to extract template variables from
   * @return a map with template variables and values
   */
  @Nullable
  Map<String, String> matchAndExtract(String pattern, Route route);

  /**
   * Given a route, return a {@link Comparator} suitable for sorting patterns
   * in order of explicitness for that route, so that more specific patterns
   * come before more generic ones.
   *
   * @param route the full path to use for comparison
   * @return a comparator capable of sorting patterns in order of explicitness
   */
  Comparator<String> getPatternComparator(Route route);

  /**
   * A parsed representation of a route.
   */
  interface Route {

    /**
     * The original route value.
     */
    String value();
  }

}
