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

package infra.web.handler.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import infra.lang.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.HandlerMapping;
import infra.web.RequestContext;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.NotAcceptableApiVersionException;
import infra.web.accept.SemanticApiVersionParser;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 16:10
 */
class VersionRequestConditionTests {

  private DefaultApiVersionStrategy strategy;

  @BeforeEach
  void setUp() {
    this.strategy = initVersionStrategy(null);
  }

  private static DefaultApiVersionStrategy initVersionStrategy(@Nullable String defaultVersion) {
    return new DefaultApiVersionStrategy(
            List.of(request -> request.getParameter("api-version")),
            new SemanticApiVersionParser(), null, defaultVersion, false, null, null);
  }

  @Test
  void combineMethodLevelOnly() {
    VersionRequestCondition condition = emptyCondition().combine(condition("1.1"));
    assertThat(condition.getVersion()).isEqualTo("1.1");
  }

  @Test
  void combineTypeLevelOnly() {
    VersionRequestCondition condition = condition("1.1").combine(emptyCondition());
    assertThat(condition.getVersion()).isEqualTo("1.1");
  }

  @Test
  void combineTypeAndMethodLevel() {
    assertThat(condition("1.1").combine(condition("1.2")).getVersion()).isEqualTo("1.2");
  }

  @Test
  void fixedVersionMatch() {
    VersionRequestCondition condition = condition("1.2");
    this.strategy.addSupportedVersion("1.1", "1.3");

    testMatch("v1.1", condition, false, false);
    testMatch("v1.2", condition, true, false);
    testMatch("v1.3", condition, true, true); // match initially, reject if chosen
  }

  @Test
  void baselineVersionMatch() {
    VersionRequestCondition condition = condition("1.2+");
    this.strategy.addSupportedVersion("1.1", "1.3");

    testMatch("v1.1", condition, false, false);
    testMatch("v1.2", condition, true, false);
    testMatch("v1.3", condition, true, false);
  }

  @Test
  void notVersionedMatch() {
    VersionRequestCondition condition = new VersionRequestCondition(null, this.strategy);
    this.strategy.addSupportedVersion("1.1", "1.3");

    testMatch("v1.1", condition, true, false);
    testMatch("v1.3", condition, true, false);
  }

  private void testMatch(
          String requestVersion, VersionRequestCondition condition, boolean matches, boolean notAcceptable) {

    RequestContext request = requestWithVersion(requestVersion);
    VersionRequestCondition match = condition.getMatchingCondition(request);

    if (!matches) {
      assertThat(match).isNull();
      return;
    }

    assertThat(match).isSameAs(condition);

    if (notAcceptable) {
      assertThatThrownBy(() -> condition.handleMatch(request))
              .isInstanceOf(NotAcceptableApiVersionException.class);
      return;
    }

    condition.handleMatch(request);
  }

  @Test
  void defaultVersion() {
    String version = "1.2";
    this.strategy = initVersionStrategy(version);
    VersionRequestCondition condition = condition(version);
    VersionRequestCondition match = condition.getMatchingCondition(exchange());

    assertThat(match).isSameAs(condition);
  }

  @Test
  void compare() {
    testCompare("1.1", "1", "1.1");
    testCompare("1.1.1", "1", "1.1", "1.1.1");
    testCompare("10", "1.1", "10");
    testCompare("10", "2", "10");
  }

  private void testCompare(String expected, String... versions) {
    List<VersionRequestCondition> list = Arrays.stream(versions)
            .map(this::condition)
            .sorted((c1, c2) -> c1.compareTo(c2, exchange()))
            .toList();

    assertThat(list.get(0)).isEqualTo(condition(expected));
  }

  @Test
  void compareWithoutRequestVersion() {
    VersionRequestCondition condition = Stream.of(condition("1.1"), condition("1.2"), emptyCondition())
            .min((c1, c2) -> c1.compareTo(c2, exchange()))
            .get();

    assertThat(condition).isEqualTo(emptyCondition());
  }

  @Test
  void noRequestVersion() {
    VersionRequestCondition condition = condition("1.1");

    MockRequestContext exchange = exchange();
    VersionRequestCondition match = condition.getMatchingCondition(exchange);
    assertThat(match).isSameAs(condition);

    condition.handleMatch(exchange);
  }

  private VersionRequestCondition condition(String v) {
    this.strategy.addSupportedVersion(v.endsWith("+") ? v.substring(0, v.length() - 1) : v);
    return new VersionRequestCondition(v, this.strategy);
  }

  private VersionRequestCondition emptyCondition() {
    return new VersionRequestCondition(null, this.strategy);
  }

  private static MockRequestContext exchange() {
    return new MockRequestContext(new HttpMockRequestImpl("GET", "/path"));
  }

  private MockRequestContext requestWithVersion(String v) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    request.setAttribute(HandlerMapping.API_VERSION_ATTRIBUTE, this.strategy.parseVersion(v));

    return new MockRequestContext(request);
  }

}