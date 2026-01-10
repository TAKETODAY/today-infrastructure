/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.condition;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static infra.http.HttpMethod.DELETE;
import static infra.http.HttpMethod.GET;
import static infra.http.HttpMethod.HEAD;
import static infra.http.HttpMethod.OPTIONS;
import static infra.http.HttpMethod.POST;
import static infra.http.HttpMethod.PUT;
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
        HttpMockRequest request = new HttpMockRequestImpl(method.name(), "");
        assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
      }
    }
    testNoMatch(condition, OPTIONS);
  }

  private MockRequestContext createContext(HttpMockRequest request) {
    return new MockRequestContext(null, request, null);
  }

  @Test
  public void getMatchingConditionWithCorsPreFlight() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl("OPTIONS", "");
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

    HttpMockRequestImpl request = new HttpMockRequestImpl();

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
    HttpMockRequestImpl request = new HttpMockRequestImpl(method.name(), "");
    RequestMethodsRequestCondition actual = condition.getMatchingCondition(createContext(request));
    assertThat(actual).isNotNull();
    assertThat(actual.getContent()).isEqualTo(Collections.singleton(method));
  }

  private void testNoMatch(RequestMethodsRequestCondition condition, HttpMethod method) {
    HttpMockRequestImpl request = new HttpMockRequestImpl(method.name(), "");
    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

}
