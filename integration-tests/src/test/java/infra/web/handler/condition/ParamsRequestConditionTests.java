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

package infra.web.handler.condition;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import infra.mock.web.HttpMockRequestImpl;
import infra.web.handler.condition.ParamsRequestCondition.ParamExpression;
import infra.web.mock.MockRequestContext;

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
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("foo", "");

    assertThat(new ParamsRequestCondition("foo").getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
  }

  @Test

  void paramPresentNullValue() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("foo", (String) null);

    assertThat(new ParamsRequestCondition("foo").getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
  }

  @Test
  void paramPresentNoMatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("bar", "");

    assertThat(new ParamsRequestCondition("foo").getMatchingCondition(new MockRequestContext(null, request, null))).isNull();
  }

  @Test
  void paramNotPresent() {
    ParamsRequestCondition condition = new ParamsRequestCondition("!foo");
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
  }

  @Test
  void paramValueMatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("foo", "bar");

    assertThat(new ParamsRequestCondition("foo=bar").getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
  }

  @Test
  void paramValueNoMatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("foo", "bazz");

    assertThat(new ParamsRequestCondition("foo=bar").getMatchingCondition(new MockRequestContext(null, request, null))).isNull();
  }

  @Test
  void compareTo() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=a", "bar");

    int result = condition1.compareTo(condition2, new MockRequestContext(null, request, null));
    assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);

    result = condition2.compareTo(condition1, new MockRequestContext(null, request, null));
    assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);
  }

  @Test

  void compareToWithMoreSpecificMatchByValue() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type=code");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

    int result = condition1.compareTo(condition2, new MockRequestContext(null, request, null));
    assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
  }

  @Test
  void compareToWithNegatedMatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type!=code");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

    assertThat(condition1.compareTo(condition2, new MockRequestContext(null, request, null)))
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
