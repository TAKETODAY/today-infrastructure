/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.handler.condition;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import cn.taketoday.web.handler.condition.ParamsRequestCondition.ParamExpression;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ParamsRequestCondition}.
 *
 * @author Arjen Poutsma
 */
public class ParamsRequestConditionTests {

  @Test
  public void paramEquals() {
    assertThat(new ParamsRequestCondition("foo")).isEqualTo(new ParamsRequestCondition("foo"));
    assertThat(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("bar"))).isFalse();
    assertThat(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("FOO"))).isFalse();
    assertThat(new ParamsRequestCondition("foo=bar")).isEqualTo(new ParamsRequestCondition("foo=bar"));
    assertThat(new ParamsRequestCondition("foo=bar").equals(new ParamsRequestCondition("FOO=bar"))).isFalse();
  }

  @Test
  public void paramPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("foo", "");

    assertThat(new ParamsRequestCondition("foo")
            .getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test // SPR-15831
  public void paramPresentNullValue() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("foo", (String) null);

    assertThat(new ParamsRequestCondition("foo").getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void paramPresentNoMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("bar", "");

    assertThat(new ParamsRequestCondition("foo").getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @Test
  public void paramNotPresent() {
    ParamsRequestCondition condition = new ParamsRequestCondition("!foo");
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void paramValueMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("foo", "bar");

    assertThat(new ParamsRequestCondition("foo=bar").getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void paramValueNoMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("foo", "bazz");

    assertThat(new ParamsRequestCondition("foo=bar").getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @Test
  public void compareTo() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=a", "bar");

    int result = condition1.compareTo(condition2, new ServletRequestContext(null, request, null));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = condition2.compareTo(condition1, new ServletRequestContext(null, request, null));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
  }

  @Test // SPR-16674
  public void compareToWithMoreSpecificMatchByValue() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type=code");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

    int result = condition1.compareTo(condition2, new ServletRequestContext(null, request, null));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();
  }

  @Test
  public void compareToWithNegatedMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type!=code");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

    assertThat(condition1.compareTo(condition2, new ServletRequestContext(null, request, null)))
            .as("Negated match should not count as more specific").isEqualTo(0);
  }

  @Test
  public void combine() {
    ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
    ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

    ParamsRequestCondition result = condition1.combine(condition2);
    Collection<ParamExpression> conditions = result.getContent();
    assertThat(conditions.size()).isEqualTo(2);
  }

}
