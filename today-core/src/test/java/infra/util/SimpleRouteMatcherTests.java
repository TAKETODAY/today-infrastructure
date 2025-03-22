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

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Map;

import infra.core.AntPathMatcher;
import infra.core.PathMatcher;
import infra.util.RouteMatcher.Route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:57
 */
class SimpleRouteMatcherTests {

  @Test
  void creationWithNullPathMatcherThrowsException() {
    assertThatThrownBy(() -> new SimpleRouteMatcher(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PathMatcher is required");
  }

  @Test
  void getPathMatcherReturnsConfiguredMatcher() {
    PathMatcher pathMatcher = new AntPathMatcher();
    SimpleRouteMatcher routeMatcher = new SimpleRouteMatcher(pathMatcher);
    assertThat(routeMatcher.getPathMatcher()).isSameAs(pathMatcher);
  }

  @Test
  void parseRouteCreatesRouteWithGivenPath() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/test/path");
    assertThat(route.value()).isEqualTo("/test/path");
  }

  @Test
  void isPatternDelegatesToPathMatcher() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    assertThat(matcher.isPattern("/test/*")).isTrue();
    assertThat(matcher.isPattern("/test/path")).isFalse();
  }

  @Test
  void combinePatternsDelegatesToPathMatcher() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    assertThat(matcher.combine("/api", "/users"))
            .isEqualTo("/api/users");
  }

  @Test
  void matchDelegatesToPathMatcher() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/test/path");
    assertThat(matcher.match("/test/*", route)).isTrue();
    assertThat(matcher.match("/other/*", route)).isFalse();
  }

  @Test
  void matchAndExtractDelegatesToPathMatcher() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/test/value");

    Map<String, String> variables = matcher.matchAndExtract("/test/{param}", route);
    assertThat(variables)
            .isNotNull()
            .containsEntry("param", "value");
  }

  @Test
  void matchAndExtractReturnsNullOnNonMatch() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/test/value");

    Map<String, String> variables = matcher.matchAndExtract("/other/{param}", route);
    assertThat(variables).isNull();
  }

  @Test
  void getPatternComparatorDelegatesToPathMatcher() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/test/path");

    Comparator<String> comparator = matcher.getPatternComparator(route);
    assertThat(comparator.compare("/test/*", "/test/**"))
            .isLessThan(0);
  }

  @Test
  void routeToStringReturnsPath() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/test/path");
    assertThat(route.toString()).isEqualTo("/test/path");
  }

  @Test
  void matchAndExtractWithMultipleVariables() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/users/123/orders/456");

    Map<String, String> variables = matcher.matchAndExtract("/users/{userId}/orders/{orderId}", route);
    assertThat(variables)
            .containsEntry("userId", "123")
            .containsEntry("orderId", "456");
  }

  @Test
  void matchAndExtractWithRegexPattern() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/users/123");

    Map<String, String> variables = matcher.matchAndExtract("/users/{id:[\\d]+}", route);
    assertThat(variables)
            .containsEntry("id", "123");
  }

  @Test
  void parseRouteWithEmptyPath() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("");
    assertThat(route.value()).isEmpty();
  }

  @Test
  void matchWithEmptyPattern() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/test");
    assertThat(matcher.match("", route)).isFalse();
  }

  @Test
  void matchHandlesAntStylePatterns() {
    SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
    Route route = matcher.parseRoute("/foo/bar/baz");

    assertThat(matcher.match("/**", route)).isTrue();
    assertThat(matcher.match("/foo/**/baz", route)).isTrue();
    assertThat(matcher.match("/foo/*/baz", route)).isTrue();
    assertThat(matcher.match("/foo/bar/?az", route)).isTrue();
  }

}