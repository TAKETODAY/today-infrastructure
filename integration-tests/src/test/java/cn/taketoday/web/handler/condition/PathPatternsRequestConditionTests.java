/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler.condition;

import org.junit.jupiter.api.Test;

import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PathPatternsRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class PathPatternsRequestConditionTests {

  private static final PathPatternParser parser = new PathPatternParser();

  @Test
  void prependSlash() {
    assertThat(createCondition("foo").getPatternValues().iterator().next())
            .isEqualTo("/foo");
  }

  @Test
  void prependNonEmptyPatternsOnly() {
    assertThat(createCondition("").getPatternValues().iterator().next())
            .as("Do not prepend empty patterns (SPR-8255)")
            .isEqualTo("");
  }

  @Test
  void getDirectUrls() {
    PathPatternsRequestCondition condition = createCondition("/something", "/else/**");
    assertThat(condition.getDirectPaths()).containsExactly("/something");
  }

  @Test
  void combineEmptySets() {
    PathPatternsRequestCondition c1 = createCondition();
    PathPatternsRequestCondition c2 = createCondition();
    PathPatternsRequestCondition c3 = c1.combine(c2);

    assertThat(c1.getPatternValues()).isSameAs(c2.getPatternValues()).containsExactly("");
    assertThat(c3.getPatternValues()).containsExactly("", "/");
  }

  @Test
  void combineOnePatternWithEmptySet() {
    PathPatternsRequestCondition c1 = createCondition("/type1", "/type2");
    PathPatternsRequestCondition c2 = createCondition();

    assertThat(c1.combine(c2)).isEqualTo(createCondition("/type1", "/type2"));

    c1 = createCondition();
    c2 = createCondition("/method1", "/method2");

    assertThat(c1.combine(c2)).isEqualTo(createCondition("/method1", "/method2"));
  }

  @Test
  void combineMultiplePatterns() {
    PathPatternsRequestCondition c1 = createCondition("/t1", "/t2");
    PathPatternsRequestCondition c2 = createCondition("/m1", "/m2");

    assertThat(c1.combine(c2)).isEqualTo(createCondition("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"));
  }

  @Test
  void matchDirectPath() {
    HttpMockRequestImpl request = createRequest("/foo");

    PathPatternsRequestCondition condition = createCondition("/foo");
    PathPatternsRequestCondition match = condition.getMatchingCondition(new MockRequestContext(null, request, null));

    assertThat(match).isNotNull();
  }

  @Test
  void matchPattern() {
    HttpMockRequestImpl request = createRequest("/foo/bar");

    PathPatternsRequestCondition condition = createCondition("/foo/*");
    PathPatternsRequestCondition match = condition.getMatchingCondition(new MockRequestContext(null, request, null));

    assertThat(match).isNotNull();
  }

  @Test
  void matchPatternWithContextPath() {
    HttpMockRequestImpl request = createRequest("", "/foo/bar");

    PathPatternsRequestCondition condition = createCondition("/foo/*");
    PathPatternsRequestCondition match = condition.getMatchingCondition(new MockRequestContext(null, request, null));

    assertThat(match).isNotNull();
  }

  @Test
  void matchSortPatterns() {
    HttpMockRequestImpl request = createRequest("/foo/bar");

    PathPatternsRequestCondition condition = createCondition("/**", "/foo/bar", "/foo/*");
    PathPatternsRequestCondition match = condition.getMatchingCondition(new MockRequestContext(null, request, null));
    PathPatternsRequestCondition expected = createCondition("/foo/bar", "/foo/*", "/**");

    assertThat(match).isEqualTo(expected);
  }

  @Test
  void matchTrailingSlash() {
    HttpMockRequestImpl request = createRequest("/foo/");

    PathPatternsRequestCondition condition = createCondition("/foo");
    PathPatternsRequestCondition match = condition.getMatchingCondition(new MockRequestContext(null, request, null));

    assertThat(match).isNotNull();
    assertThat(match.getPatternValues().iterator().next()).as("Should match by default").isEqualTo("/foo");

    PathPatternParser strictParser = new PathPatternParser();
    strictParser.setMatchOptionalTrailingSeparator(false);

    condition = new PathPatternsRequestCondition(strictParser, "/foo");
    match = condition.getMatchingCondition(new MockRequestContext(null, request, null));

    assertThat(match).isNull();
  }

  @Test
  void matchPatternContainsExtension() {
    HttpMockRequestImpl request = createRequest("/foo.html");
    PathPatternsRequestCondition match = createCondition("/foo.jpg")
            .getMatchingCondition(new MockRequestContext(null, request, null));

    assertThat(match).isNull();
  }

  @Test
    // gh-22543
  void matchWithEmptyPatterns() {
    PathPatternsRequestCondition condition = createCondition();
    assertThat(condition.getMatchingCondition(createContext(""))).isNotNull();
    assertThat(condition.getMatchingCondition(createContext("/anything"))).isNull();

    condition = condition.combine(createCondition());
    assertThat(condition.getMatchingCondition(createContext(""))).isNotNull();
    assertThat(condition.getMatchingCondition(createContext("/anything"))).isNull();
  }

  @Test
  void compareEqualPatterns() {
    PathPatternsRequestCondition c1 = createCondition("/foo*");
    PathPatternsRequestCondition c2 = createCondition("/foo*");

    assertThat(c1.compareTo(c2, createContext("/foo"))).isEqualTo(0);
  }

  @Test
  void comparePatternSpecificity() {
    PathPatternsRequestCondition c1 = createCondition("/fo*");
    PathPatternsRequestCondition c2 = createCondition("/foo");

    assertThat(c1.compareTo(c2, createContext("/foo"))).isEqualTo(1);
  }

  @Test
  void compareNumberOfMatchingPatterns() {
    HttpMockRequest request = createRequest("/foo");

    PathPatternsRequestCondition c1 = createCondition("/foo", "/bar");
    PathPatternsRequestCondition c2 = createCondition("/foo", "/f*");

    PathPatternsRequestCondition match1 = c1.getMatchingCondition(new MockRequestContext(null, request, null));
    PathPatternsRequestCondition match2 = c2.getMatchingCondition(new MockRequestContext(null, request, null));

    assertThat(match1.compareTo(match2, new MockRequestContext(null, request, null))).isEqualTo(1);
  }

  private HttpMockRequestImpl createRequest(String requestURI) {
    return createRequest("", requestURI);
  }

  private MockRequestContext createContext(String requestURI) {
    return new MockRequestContext(null, createRequest("", requestURI), null);
  }

  private MockRequestContext createContext(String contextPath, String requestURI) {
    return new MockRequestContext(null, createRequest(contextPath, requestURI), null);
  }

  private HttpMockRequestImpl createRequest(String contextPath, String requestURI) {
    return new HttpMockRequestImpl("GET", requestURI);
  }

  private PathPatternsRequestCondition createCondition(String... patterns) {
    return new PathPatternsRequestCondition(parser, patterns);
  }
}
