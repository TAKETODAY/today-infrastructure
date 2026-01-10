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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.http.HttpMethod;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

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
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setParameter("param1", "paramValue1");
    request.addHeader("header1", "headerValue1");

    RequestCondition<?> getPostCond = new RequestMethodsRequestCondition(HttpMethod.GET, HttpMethod.POST);
    RequestCondition<?> getCond = new RequestMethodsRequestCondition(HttpMethod.GET);

    CompositeRequestCondition condition = new CompositeRequestCondition(this.param1, getPostCond);
    CompositeRequestCondition matchingCondition = new CompositeRequestCondition(this.param1, getCond);
    MockRequestContext context = new MockRequestContext(null, request, null);

    assertThat(condition.getMatchingCondition(context)).isEqualTo(matchingCondition);
  }

  @Test
  public void noMatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    CompositeRequestCondition cond = new CompositeRequestCondition(this.param1);
    MockRequestContext context = new MockRequestContext(null, request, null);

    assertThat(cond.getMatchingCondition(context)).isNull();
  }

  @Test
  public void matchEmpty() {
    CompositeRequestCondition empty = new CompositeRequestCondition();
    MockRequestContext context = new MockRequestContext(null, new HttpMockRequestImpl(), null);

    assertThat(empty.getMatchingCondition(context)).isSameAs(empty);
  }

  @Test
  public void compare() {
    HttpMockRequest request = new HttpMockRequestImpl();

    CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
    CompositeRequestCondition cond3 = new CompositeRequestCondition(this.param3);
    MockRequestContext context = new MockRequestContext(null, request, null);
    assertThat(cond1.compareTo(cond3, context)).isEqualTo(1);
    assertThat(cond3.compareTo(cond1, context)).isEqualTo(-1);
  }

  @Test
  public void compareEmpty() {
    HttpMockRequest request = new HttpMockRequestImpl();

    CompositeRequestCondition empty = new CompositeRequestCondition();
    CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);
    MockRequestContext context = new MockRequestContext(null, request, null);

    assertThat(empty.compareTo(empty, context)).isEqualTo(0);
    assertThat(notEmpty.compareTo(empty, context)).isEqualTo(-1);
    assertThat(empty.compareTo(notEmpty, context)).isEqualTo(1);
  }

  @Test
  public void compareDifferentLength() {
    MockRequestContext context = new MockRequestContext(null, new HttpMockRequestImpl(), null);

    CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
    CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
    assertThatIllegalArgumentException().isThrownBy(() ->
            cond1.compareTo(cond2, context));
  }

}
