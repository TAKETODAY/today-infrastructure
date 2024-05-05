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

import java.util.Collection;

import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.web.handler.condition.ParamsRequestCondition.ParamExpression;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ParamsRequestCondition}.
 *
 * @author Arjen Poutsma
 */
public class ParamsRequestConditionTests {

  @Test
  void paramEquals() {
    assertThat(new ParamsRequestCondition("foo")).isEqualTo(new ParamsRequestCondition("foo"));
    assertThat(new ParamsRequestCondition("foo")).isNotEqualTo(new ParamsRequestCondition("bar"));
    assertThat(new ParamsRequestCondition("foo")).isNotEqualTo(new ParamsRequestCondition("FOO"));
    assertThat(new ParamsRequestCondition("foo=bar")).isEqualTo(new ParamsRequestCondition("foo=bar"));
    assertThat(new ParamsRequestCondition("foo=bar")).isNotEqualTo(new ParamsRequestCondition("FOO=bar"));
  }

  @Test
  void paramPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("foo", "");

    assertThat(new ParamsRequestCondition("foo").getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
    // SPR-15831
  void paramPresentNullValue() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("foo", (String) null);

    assertThat(new ParamsRequestCondition("foo").getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  void paramPresentNoMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("bar", "");

    assertThat(new ParamsRequestCondition("foo").getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @Test
  void paramNotPresent() {
    ParamsRequestCondition condition = new ParamsRequestCondition("!foo");
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  void paramValueMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("foo", "bar");

    assertThat(new ParamsRequestCondition("foo=bar").getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  void paramValueNoMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("foo", "bazz");

    assertThat(new ParamsRequestCondition("foo=bar").getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @Test
  void compareTo() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=a", "bar");

    int result = condition1.compareTo(condition2, new ServletRequestContext(null, request, null));
    assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

    result = condition2.compareTo(condition1, new ServletRequestContext(null, request, null));
    assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
  }

  @Test
    // SPR-16674
  void compareToWithMoreSpecificMatchByValue() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type=code");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

    int result = condition1.compareTo(condition2, new ServletRequestContext(null, request, null));
    assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
  }

  @Test
  void compareToWithNegatedMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type!=code");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

    assertThat(condition1.compareTo(condition2, new ServletRequestContext(null, request, null)))
            .as("Negated match should not count as more specific").isEqualTo(0);
  }

  @Test
  void combineWithOtherEmpty() {
    ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
    ParamsRequestCondition condition2 = new ParamsRequestCondition();

    ParamsRequestCondition result = condition1.combine(condition2);
    assertThat(result).isEqualTo(condition1);
  }

  @Test
  void combineWithThisEmpty() {
    ParamsRequestCondition condition1 = new ParamsRequestCondition();
    ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=bar");

    ParamsRequestCondition result = condition1.combine(condition2);
    assertThat(result).isEqualTo(condition2);
  }

  @Test
  void combine() {
    ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

    ParamsRequestCondition result = condition1.combine(condition2);
    Collection<ParamExpression> conditions = result.getContent();
    assertThat(conditions).hasSize(2);
  }

}
