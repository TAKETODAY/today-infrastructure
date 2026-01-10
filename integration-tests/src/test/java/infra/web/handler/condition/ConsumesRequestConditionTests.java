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

import java.util.Collection;
import java.util.Collections;

import infra.http.HttpHeaders;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Arjen Poutsma
 * @since 4.0 2022/3/31 12:40
 */
public class ConsumesRequestConditionTests {

  @Test
  public void consumesMatch() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setContentType("text/plain");

    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void negatedConsumesMatch() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setContentType("text/plain");

    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNull();
  }

  @Test
  public void getConsumableMediaTypesNegatedExpression() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("!application/xml");
    assertThat(condition.getConsumableMediaTypes()).isEqualTo(Collections.emptySet());
  }

  @Test
  public void consumesWildcardMatch() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("text/*");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setContentType("text/plain");

    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void consumesMultipleMatch() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setContentType("text/plain");

    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void consumesSingleNoMatch() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setContentType("application/xml");

    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNull();
  }

  @Test // gh-28024
  public void matchWithParameters() {
    String base = "application/hal+json";
    ConsumesRequestCondition condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setContentType(base + ";profile=\"a\"");
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();

    condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
    request.setContentType(base + ";profile=\"b\"");
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNull();

    condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
    request.setContentType(base);
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();

    condition = new ConsumesRequestCondition(base);
    request.setContentType(base + ";profile=\"a\"");
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();

    condition = new ConsumesRequestCondition(base + ";profile=\"a\"");
    request.setContentType(base + ";profile=\"A\"");
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
  }

  @Test
  public void consumesParseError() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setContentType("01");

    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNull();
  }

  @Test
  public void consumesParseErrorWithNegation() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("!text/plain");

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setContentType("01");

    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNull();
  }

  @Test // gh-22010
  public void consumesNoContent() {
    ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain");
    condition.setBodyRequired(false);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();
    request = new HttpMockRequestImpl("GET", "/");
    request.addHeader(HttpHeaders.CONTENT_LENGTH, "0");
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();

    request = new HttpMockRequestImpl("GET", "/");
    request.addHeader(HttpHeaders.CONTENT_LENGTH, "21");
    request.setContent(new byte[21]);
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNull();

    request = new HttpMockRequestImpl("GET", "/");
    request.addHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
    assertThat(condition.getMatchingCondition(new MockRequestContext(null, request, null))).isNull();
  }

  @Test
  public void compareToSingle() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
    ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*");

    int result = condition1.compareTo(condition2, new MockRequestContext(null, request, null));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = condition2.compareTo(condition1, new MockRequestContext(null, request, null));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
  }

  @Test
  public void compareToMultiple() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    ConsumesRequestCondition condition1 = new ConsumesRequestCondition("*/*", "text/plain");
    ConsumesRequestCondition condition2 = new ConsumesRequestCondition("text/*", "text/plain;q=0.7");

    int result = condition1.compareTo(condition2, new MockRequestContext(null, request, null));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = condition2.compareTo(condition1, new MockRequestContext(null, request, null));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
  }

  @Test
  public void combine() {
    ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
    ConsumesRequestCondition condition2 = new ConsumesRequestCondition("application/xml");

    ConsumesRequestCondition result = condition1.combine(condition2);
    assertThat(result).isEqualTo(condition2);
  }

  @Test
  public void combineWithDefault() {
    ConsumesRequestCondition condition1 = new ConsumesRequestCondition("text/plain");
    ConsumesRequestCondition condition2 = new ConsumesRequestCondition();

    ConsumesRequestCondition result = condition1.combine(condition2);
    assertThat(result).isEqualTo(condition1);
  }

  @Test
  public void parseConsumesAndHeaders() {
    String[] consumes = new String[] { "text/plain" };
    String[] headers = new String[] { "foo=bar", "content-type=application/xml,application/pdf" };
    ConsumesRequestCondition condition = new ConsumesRequestCondition(consumes, headers);

    assertConditions(condition, "text/plain", "application/xml", "application/pdf");
  }

  @Test
  public void getMatchingCondition() {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.setContentType("text/plain");

    ConsumesRequestCondition condition = new ConsumesRequestCondition("text/plain", "application/xml");

    ConsumesRequestCondition result = condition.getMatchingCondition(new MockRequestContext(null, request, null));
    assertConditions(result, "text/plain");

    condition = new ConsumesRequestCondition("application/xml");

    result = condition.getMatchingCondition(new MockRequestContext(null, request, null));
    assertThat(result).isNull();
  }

  private void assertConditions(ConsumesRequestCondition condition, String... expected) {
    Collection<MediaTypeExpression> expressions = condition.getContent();
    assertThat(expressions.stream().map(expr -> expr.mediaType.toString()))
            .containsExactlyInAnyOrder(expected);
  }

}
