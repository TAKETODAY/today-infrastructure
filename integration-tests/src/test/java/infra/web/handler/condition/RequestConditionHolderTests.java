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

import infra.http.HttpMethod;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

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
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setParameter("name1", "value1");

    RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(HttpMethod.GET, HttpMethod.POST);
    RequestConditionHolder custom = new RequestConditionHolder(rm);
    RequestMethodsRequestCondition expected = new RequestMethodsRequestCondition(HttpMethod.GET);

    assertThat(custom.getMatchingCondition(createContext(request)).getCondition()).isEqualTo(expected);
  }

  @Test
  public void noMatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");

    RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(HttpMethod.POST);
    RequestConditionHolder custom = new RequestConditionHolder(rm);

    assertThat(custom.getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void matchEmpty() {
    RequestConditionHolder empty = new RequestConditionHolder(null);
    assertThat(empty.getMatchingCondition(createContext(new HttpMockRequestImpl()))).isSameAs(empty);
  }

  @Test
  public void compare() {
    HttpMockRequest request = new HttpMockRequestImpl();

    RequestConditionHolder params11 = new RequestConditionHolder(new ParamsRequestCondition("1"));
    RequestConditionHolder params12 = new RequestConditionHolder(new ParamsRequestCondition("1", "2"));

    assertThat(params11.compareTo(params12, createContext(request))).isEqualTo(1);
    assertThat(params12.compareTo(params11, createContext(request))).isEqualTo(-1);
  }

  @Test
  public void compareEmpty() {
    HttpMockRequest request = new HttpMockRequestImpl();

    RequestConditionHolder empty = new RequestConditionHolder(null);
    RequestConditionHolder empty2 = new RequestConditionHolder(null);
    RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

    assertThat(empty.compareTo(empty2, createContext(request))).isEqualTo(0);
    assertThat(notEmpty.compareTo(empty, createContext(request))).isEqualTo(-1);
    assertThat(empty.compareTo(notEmpty, createContext(request))).isEqualTo(1);
  }

  private MockRequestContext createContext(HttpMockRequest request) {
    return new MockRequestContext(null, request, null);
  }

  @Test
  public void compareIncompatible() {
    RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
    RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
            params.compareTo(headers, createContext(new HttpMockRequestImpl())));
  }

}
