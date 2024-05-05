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

import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.web.handler.condition.HeadersRequestCondition.HeaderExpression;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class HeadersRequestConditionTests {

  @Test
  public void headerEquals() {
    assertThat(new HeadersRequestCondition("foo")).isEqualTo(new HeadersRequestCondition("foo"));
    assertThat(new HeadersRequestCondition("FOO")).isEqualTo(new HeadersRequestCondition("foo"));
    assertThat(new HeadersRequestCondition("bar")).isNotEqualTo(new HeadersRequestCondition("foo"));
    assertThat(new HeadersRequestCondition("foo=bar")).isEqualTo(new HeadersRequestCondition("foo=bar"));
    assertThat(new HeadersRequestCondition("FOO=bar")).isEqualTo(new HeadersRequestCondition("foo=bar"));
  }

  @Test
  public void headerPresent() {
    HeadersRequestCondition condition = new HeadersRequestCondition("accept");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("Accept", "");

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void headerPresentNoMatch() {
    HeadersRequestCondition condition = new HeadersRequestCondition("foo");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("bar", "");

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @Test
  public void headerNotPresent() {
    HeadersRequestCondition condition = new HeadersRequestCondition("!accept");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void headerValueMatch() {
    HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("foo", "bar");

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void headerValueNoMatch() {
    HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("foo", "bazz");

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @Test
  public void headerCaseSensitiveValueMatch() {
    HeadersRequestCondition condition = new HeadersRequestCondition("foo=Bar");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("foo", "bar");

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @Test
  public void headerValueMatchNegated() {
    HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("foo", "baz");

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void headerValueNoMatchNegated() {
    HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("foo", "bar");

    assertThat(condition.getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @Test
  public void compareTo() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    HeadersRequestCondition condition1 = new HeadersRequestCondition("foo", "bar", "baz");
    HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=a", "bar");

    int result = condition1.compareTo(condition2, new ServletRequestContext(null, request, null));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = condition2.compareTo(condition1, new ServletRequestContext(null, request, null));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
  }

  @Test // SPR-16674
  public void compareToWithMoreSpecificMatchByValue() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=a");
    HeadersRequestCondition condition2 = new HeadersRequestCondition("foo");

    int result = condition1.compareTo(condition2, new ServletRequestContext(null, request, null));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = condition2.compareTo(condition1, new ServletRequestContext(null, request, null));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
  }

  @Test
  public void compareToWithNegatedMatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    HeadersRequestCondition condition1 = new HeadersRequestCondition("foo!=a");
    HeadersRequestCondition condition2 = new HeadersRequestCondition("foo");

    assertThat(condition1.compareTo(condition2, new ServletRequestContext(null, request, null)))
            .as("Negated match should not count as more specific")
            .isEqualTo(0);
  }

  @Test
  public void combine() {
    HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=bar");
    HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=baz");

    HeadersRequestCondition result = condition1.combine(condition2);
    Collection<HeaderExpression> conditions = result.getContent();
    assertThat(conditions.size()).isEqualTo(2);
  }

  @Test
  public void getMatchingCondition() {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("foo", "bar");

    HeadersRequestCondition condition = new HeadersRequestCondition("foo");

    HeadersRequestCondition result = condition.getMatchingCondition(new ServletRequestContext(null, request, null));
    assertThat(result).isEqualTo(condition);

    condition = new HeadersRequestCondition("bar");

    result = condition.getMatchingCondition(new ServletRequestContext(null, request, null));
    assertThat(result).isNull();
  }

}
