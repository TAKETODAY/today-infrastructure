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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import infra.mock.web.HttpMockRequestImpl;
import infra.web.RequestContext;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.NotAcceptableApiVersionException;
import infra.web.accept.SemanticApiVersionParser;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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

  private static DefaultApiVersionStrategy initVersionStrategy(@Nullable String defaultValue) {
    return new DefaultApiVersionStrategy(
            List.of(exchange -> exchange.getParameters().getFirst("api-version")),
            new SemanticApiVersionParser(), true, defaultValue, false);
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
    String conditionVersion = "1.2";
    this.strategy.addSupportedVersion("1.1", "1.3");

    testMatch("v1.1", conditionVersion, true, false);
    testMatch("v1.2", conditionVersion, false, false);
    testMatch("v1.3", conditionVersion, false, true);
  }

  @Test
  void baselineVersionMatch() {
    String conditionVersion = "1.2+";
    this.strategy.addSupportedVersion("1.1", "1.3");

    testMatch("v1.1", conditionVersion, true, false);
    testMatch("v1.2", conditionVersion, false, false);
    testMatch("v1.3", conditionVersion, false, false);
  }

  private void testMatch(
          String requestVersion, String conditionVersion, boolean notCompatible, boolean notAcceptable) {

    RequestContext exchange = exchangeWithVersion(requestVersion);
    VersionRequestCondition condition = condition(conditionVersion);
    VersionRequestCondition match = condition.getMatchingCondition(exchange);

    if (notCompatible) {
      assertThat(match).isNull();
      return;
    }

    assertThat(match).isSameAs(condition);

    if (notAcceptable) {
      assertThatThrownBy(() -> condition.handleMatch(exchange)).isInstanceOf(NotAcceptableApiVersionException.class);
      return;
    }

    condition.handleMatch(exchange);
  }

  @Test
  void missingRequiredVersion() {
    assertThatThrownBy(() -> condition("1.2").getMatchingCondition(exchange()))
            .hasMessage("400 BAD_REQUEST \"API version is required.\"");
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
  void unsupportedVersion() {
    assertThatThrownBy(() -> condition("1.2").getMatchingCondition(exchangeWithVersion("1.3")))
            .hasMessage("400 BAD_REQUEST \"Invalid API version: '1.3.0'.\"");
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

  private VersionRequestCondition condition(String v) {
    this.strategy.addSupportedVersion(v.endsWith("+") ? v.substring(0, v.length() - 1) : v);
    return new VersionRequestCondition(v, this.strategy);
  }

  private VersionRequestCondition emptyCondition() {
    return new VersionRequestCondition();
  }

  private static MockRequestContext exchange() {
    return new MockRequestContext(new HttpMockRequestImpl("GET", "/path"));
  }

  private MockRequestContext exchangeWithVersion(String v) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    request.addParameter("api-version", v);
    return new MockRequestContext(request);
  }

}