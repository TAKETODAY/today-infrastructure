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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.mock.api.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/31 12:40
 */
class CompositeRequestConditionTests {

  private ParamsRequestCondition param1;
  private ParamsRequestCondition param2;
  private ParamsRequestCondition param3;

  private HeadersRequestCondition header1;
  private HeadersRequestCondition header2;
  private HeadersRequestCondition header3;

  @BeforeEach
  public void setup() {
    this.param1 = new ParamsRequestCondition("param1");
    this.param2 = new ParamsRequestCondition("param2");
    this.param3 = this.param1.combine(this.param2);

    this.header1 = new HeadersRequestCondition("header1");
    this.header2 = new HeadersRequestCondition("header2");
    this.header3 = this.header1.combine(this.header2);
  }

  @Test
  public void combine() {
    CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1, this.header1);
    CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param2, this.header2);
    CompositeRequestCondition cond3 = new CompositeRequestCondition(this.param3, this.header3);

    assertThat(cond1.combine(cond2)).isEqualTo(cond3);
  }

  @Test
  public void combineEmpty() {
    CompositeRequestCondition empty = new CompositeRequestCondition();
    CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);

    assertThat(empty.combine(empty)).isSameAs(empty);
    assertThat(notEmpty.combine(empty)).isSameAs(notEmpty);
    assertThat(empty.combine(notEmpty)).isSameAs(notEmpty);
  }

  @Test
  public void combineDifferentLength() {
    CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
    CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
    assertThatIllegalArgumentException().isThrownBy(() ->
            cond1.combine(cond2));
  }

  @Test
  public void match() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    request.setParameter("param1", "paramValue1");
    request.addHeader("header1", "headerValue1");

    RequestCondition<?> getPostCond = new RequestMethodsRequestCondition(HttpMethod.GET, HttpMethod.POST);
    RequestCondition<?> getCond = new RequestMethodsRequestCondition(HttpMethod.GET);

    CompositeRequestCondition condition = new CompositeRequestCondition(this.param1, getPostCond);
    CompositeRequestCondition matchingCondition = new CompositeRequestCondition(this.param1, getCond);
    ServletRequestContext context = new ServletRequestContext(null, request, null);

    assertThat(condition.getMatchingCondition(context)).isEqualTo(matchingCondition);
  }

  @Test
  public void noMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    CompositeRequestCondition cond = new CompositeRequestCondition(this.param1);
    ServletRequestContext context = new ServletRequestContext(null, request, null);

    assertThat(cond.getMatchingCondition(context)).isNull();
  }

  @Test
  public void matchEmpty() {
    CompositeRequestCondition empty = new CompositeRequestCondition();
    ServletRequestContext context = new ServletRequestContext(null, new MockHttpServletRequest(), null);

    assertThat(empty.getMatchingCondition(context)).isSameAs(empty);
  }

  @Test
  public void compare() {
    HttpServletRequest request = new MockHttpServletRequest();

    CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
    CompositeRequestCondition cond3 = new CompositeRequestCondition(this.param3);
    ServletRequestContext context = new ServletRequestContext(null, request, null);
    assertThat(cond1.compareTo(cond3, context)).isEqualTo(1);
    assertThat(cond3.compareTo(cond1, context)).isEqualTo(-1);
  }

  @Test
  public void compareEmpty() {
    HttpServletRequest request = new MockHttpServletRequest();

    CompositeRequestCondition empty = new CompositeRequestCondition();
    CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);
    ServletRequestContext context = new ServletRequestContext(null, request, null);

    assertThat(empty.compareTo(empty, context)).isEqualTo(0);
    assertThat(notEmpty.compareTo(empty, context)).isEqualTo(-1);
    assertThat(empty.compareTo(notEmpty, context)).isEqualTo(1);
  }

  @Test
  public void compareDifferentLength() {
    ServletRequestContext context = new ServletRequestContext(null, new MockHttpServletRequest(), null);

    CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
    CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
    assertThatIllegalArgumentException().isThrownBy(() ->
            cond1.compareTo(cond2, context));
  }

}
