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

import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.NonNull;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * A test fixture for {@link RequestConditionHolder} tests.
 *
 * @author Rossen Stoyanchev
 */
public class RequestConditionHolderTests {

  @Test
  public void combine() {
    RequestConditionHolder params1 = new RequestConditionHolder(new ParamsRequestCondition("name1"));
    RequestConditionHolder params2 = new RequestConditionHolder(new ParamsRequestCondition("name2"));
    RequestConditionHolder expected = new RequestConditionHolder(new ParamsRequestCondition("name1", "name2"));

    assertThat(params1.combine(params2)).isEqualTo(expected);
  }

  @Test
  public void combineEmpty() {
    RequestConditionHolder empty = new RequestConditionHolder(null);
    RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

    assertThat(empty.combine(empty)).isSameAs(empty);
    assertThat(notEmpty.combine(empty)).isSameAs(notEmpty);
    assertThat(empty.combine(notEmpty)).isSameAs(notEmpty);
  }

  @Test
  public void combineIncompatible() {
    RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
    RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
            params.combine(headers));
  }

  @Test
  public void match() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    request.setParameter("name1", "value1");

    RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(HttpMethod.GET, HttpMethod.POST);
    RequestConditionHolder custom = new RequestConditionHolder(rm);
    RequestMethodsRequestCondition expected = new RequestMethodsRequestCondition(HttpMethod.GET);

    assertThat(custom.getMatchingCondition(createContext(request)).getCondition()).isEqualTo(expected);
  }

  @Test
  public void noMatch() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

    RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(HttpMethod.POST);
    RequestConditionHolder custom = new RequestConditionHolder(rm);

    assertThat(custom.getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void matchEmpty() {
    RequestConditionHolder empty = new RequestConditionHolder(null);
    assertThat(empty.getMatchingCondition(createContext(new MockHttpServletRequest()))).isSameAs(empty);
  }

  @Test
  public void compare() {
    HttpServletRequest request = new MockHttpServletRequest();

    RequestConditionHolder params11 = new RequestConditionHolder(new ParamsRequestCondition("1"));
    RequestConditionHolder params12 = new RequestConditionHolder(new ParamsRequestCondition("1", "2"));

    assertThat(params11.compareTo(params12, createContext(request))).isEqualTo(1);
    assertThat(params12.compareTo(params11, createContext(request))).isEqualTo(-1);
  }

  @Test
  public void compareEmpty() {
    HttpServletRequest request = new MockHttpServletRequest();

    RequestConditionHolder empty = new RequestConditionHolder(null);
    RequestConditionHolder empty2 = new RequestConditionHolder(null);
    RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

    assertThat(empty.compareTo(empty2, createContext(request))).isEqualTo(0);
    assertThat(notEmpty.compareTo(empty, createContext(request))).isEqualTo(-1);
    assertThat(empty.compareTo(notEmpty, createContext(request))).isEqualTo(1);
  }

  @NonNull
  private ServletRequestContext createContext(HttpServletRequest request) {
    return new ServletRequestContext(null, request, null);
  }

  @Test
  public void compareIncompatible() {
    RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
    RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
            params.compareTo(headers, createContext(new MockHttpServletRequest())));
  }

}
