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

import java.util.Collections;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.NonNull;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.mock.http.HttpServletRequest;

import static cn.taketoday.http.HttpMethod.DELETE;
import static cn.taketoday.http.HttpMethod.GET;
import static cn.taketoday.http.HttpMethod.HEAD;
import static cn.taketoday.http.HttpMethod.OPTIONS;
import static cn.taketoday.http.HttpMethod.POST;
import static cn.taketoday.http.HttpMethod.PUT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMethodsRequestConditionTests {

  @Test
  public void getMatchingCondition() {
    testMatch(new RequestMethodsRequestCondition(GET), GET);
    testMatch(new RequestMethodsRequestCondition(GET, POST), GET);
    testNoMatch(new RequestMethodsRequestCondition(GET), POST);
  }

  @Test
  public void getMatchingConditionWithHttpHead() {
    testMatch(new RequestMethodsRequestCondition(HEAD), HEAD);
    testMatch(new RequestMethodsRequestCondition(GET), GET);
    testNoMatch(new RequestMethodsRequestCondition(POST), HEAD);
  }

  @Test
  public void getMatchingConditionWithEmptyConditions() {
    RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition();
    for (HttpMethod method : HttpMethod.values()) {
      if (method != OPTIONS) {
        HttpServletRequest request = new MockHttpServletRequest(method.name(), "");
        assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
      }
    }
    testNoMatch(condition, OPTIONS);
  }

  @NonNull
  private ServletRequestContext createContext(HttpServletRequest request) {
    return new ServletRequestContext(null, request, null);
  }

  @Test
  public void getMatchingConditionWithCorsPreFlight() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "");
    request.addHeader("Origin", "https://example.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");

    assertThat(new RequestMethodsRequestCondition().getMatchingCondition(createContext(request))).isNotNull();
    assertThat(new RequestMethodsRequestCondition(PUT).getMatchingCondition(createContext(request))).isNotNull();
    assertThat(new RequestMethodsRequestCondition(DELETE).getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void compareTo() {
    RequestMethodsRequestCondition c1 = new RequestMethodsRequestCondition(GET, HEAD);
    RequestMethodsRequestCondition c2 = new RequestMethodsRequestCondition(POST);
    RequestMethodsRequestCondition c3 = new RequestMethodsRequestCondition();

    MockHttpServletRequest request = new MockHttpServletRequest();

    int result = c1.compareTo(c2, createContext(request));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = c2.compareTo(c1, createContext(request));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();

    result = c2.compareTo(c3, createContext(request));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = c1.compareTo(c1, createContext(request));
    assertThat(result).as("Invalid comparison result ").isEqualTo(0);
  }

  @Test
  public void combine() {
    RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(GET);
    RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(POST);

    RequestMethodsRequestCondition result = condition1.combine(condition2);
    assertThat(result.getContent().size()).isEqualTo(2);
  }

  private void testMatch(RequestMethodsRequestCondition condition, HttpMethod method) {
    MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "");
    RequestMethodsRequestCondition actual = condition.getMatchingCondition(createContext(request));
    assertThat(actual).isNotNull();
    assertThat(actual.getContent()).isEqualTo(Collections.singleton(method));
  }

  private void testNoMatch(RequestMethodsRequestCondition condition, HttpMethod method) {
    MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "");
    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

}
