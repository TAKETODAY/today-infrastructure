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

import java.util.Arrays;
import java.util.Collection;

import infra.http.MediaType;
import infra.web.mock.MockRequest;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.FixedContentNegotiationStrategy;
import infra.web.accept.HeaderContentNegotiationStrategy;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProducesRequestCondition}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ProducesRequestConditionTests {

  @Test
  public void match() {
    ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");
    MockRequest request = createRequest("text/plain");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test
  public void matchNegated() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");
    MockRequest request = createRequest("text/plain");

    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void matchNegatedWithoutAcceptHeader() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");
    MockRequest request = new MockRequest("GET", "/");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
    assertThat(condition.getProducibleMediaTypes()).isEmpty();
  }

  @Test
  public void getProducibleMediaTypes() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!application/xml");
    assertThat(condition.getProducibleMediaTypes()).isEmpty();
  }

  @Test
  public void matchWildcard() {
    ProducesRequestCondition condition = new ProducesRequestCondition("text/*");
    MockRequest request = createRequest("text/plain");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test
  public void matchMultiple() {
    ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");
    MockRequest request = createRequest("text/plain");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test
  public void matchSingle() {
    ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");
    MockRequest request = createRequest("application/xml");

    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

  @Test // gh-21670
  public void matchWithParameters() {
    String base = "application/atom+xml";
    ProducesRequestCondition condition = new ProducesRequestCondition(base + ";type=feed");
    MockRequest request = createRequest(base + ";type=entry");
    assertThat(condition.getMatchingCondition(createContext(request))).isNull();

    condition = new ProducesRequestCondition(base + ";type=feed");
    request = createRequest(base + ";type=feed");
    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();

    condition = new ProducesRequestCondition(base + ";type=feed");
    request = createRequest(base);
    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();

    condition = new ProducesRequestCondition(base);
    request = createRequest(base + ";type=feed");
    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test
  public void matchParseError() {
    ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");
    MockRequest request = createRequest("bogus");

    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void matchParseErrorWithNegation() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");
    MockRequest request = createRequest("bogus");

    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void matchByRequestParameter() {
    String[] produces = { "text/plain" };
    String[] headers = {};
    ProducesRequestCondition condition = new ProducesRequestCondition(produces, headers);
    MockRequest request = new MockRequest("GET", "/foo.txt");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test
  public void matchWithNegationAndMediaTypeAllWithQualityParameter() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!application/json");
    MockRequest request = createRequest(
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test // gh-22853
  public void matchAndCompare() {
    ContentNegotiationManager manager = new ContentNegotiationManager(
            new HeaderContentNegotiationStrategy(),
            new FixedContentNegotiationStrategy(MediaType.TEXT_HTML));

    ProducesRequestCondition none = new ProducesRequestCondition(new String[0], null, manager);
    ProducesRequestCondition html = new ProducesRequestCondition(new String[] { "text/html" }, null, manager);

    MockRequest request = new MockRequest("GET", "/");
    request.addHeader("Accept", "*/*");

    ProducesRequestCondition noneMatch = none.getMatchingCondition(createContext(request));
    ProducesRequestCondition htmlMatch = html.getMatchingCondition(createContext(request));

    assertThat(noneMatch.compareTo(htmlMatch, createContext(request))).isEqualTo(1);
  }

  @Test
  public void compareTo() {
    ProducesRequestCondition html = new ProducesRequestCondition("text/html");
    ProducesRequestCondition xml = new ProducesRequestCondition("application/xml");
    ProducesRequestCondition none = new ProducesRequestCondition();

    MockRequest request = createRequest("application/xml, text/html");

    assertThat(html.compareTo(xml, createContext(request)) > 0).isTrue();
    assertThat(xml.compareTo(html, createContext(request)) < 0).isTrue();
    assertThat(xml.compareTo(none, createContext(request)) < 0).isTrue();
    assertThat(none.compareTo(xml, createContext(request)) > 0).isTrue();
    assertThat(html.compareTo(none, createContext(request)) < 0).isTrue();
    assertThat(none.compareTo(html, createContext(request)) > 0).isTrue();

    request = createRequest("application/xml, text/*");

    assertThat(html.compareTo(xml, createContext(request)) > 0).isTrue();
    assertThat(xml.compareTo(html, createContext(request)) < 0).isTrue();

    request = createRequest("application/pdf");

    assertThat(html.compareTo(xml, createContext(request))).isEqualTo(0);
    assertThat(xml.compareTo(html, createContext(request))).isEqualTo(0);

    request = createRequest("text/html;q=0.9,application/xml");

    assertThat(html.compareTo(xml, createContext(request)) > 0).isTrue();
    assertThat(xml.compareTo(html, createContext(request)) < 0).isTrue();
  }

  @Test
  public void compareToWithSingleExpression() {
    MockRequest request = createRequest("text/plain");

    ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
    ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*");

    int result = condition1.compareTo(condition2, createContext(request));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = condition2.compareTo(condition1, createContext(request));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
  }

  @Test
  public void compareToMultipleExpressions() {
    ProducesRequestCondition condition1 = new ProducesRequestCondition("*/*", "text/plain");
    ProducesRequestCondition condition2 = new ProducesRequestCondition("text/*", "text/plain;q=0.7");

    MockRequest request = createRequest("text/plain");

    int result = condition1.compareTo(condition2, createContext(request));
    assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);

    result = condition2.compareTo(condition1, createContext(request));
    assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
  }

  @Test
  public void compareToMultipleExpressionsAndMultipleAcceptHeaderValues() {
    ProducesRequestCondition condition1 = new ProducesRequestCondition("text/*", "text/plain");
    ProducesRequestCondition condition2 = new ProducesRequestCondition("application/*", "application/xml");

    MockRequest request = createRequest("text/plain", "application/xml");

    int result = condition1.compareTo(condition2, createContext(request));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

    result = condition2.compareTo(condition1, createContext(request));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();

    request = createRequest("application/xml", "text/plain");

    result = condition1.compareTo(condition2, createContext(request));
    assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();

    result = condition2.compareTo(condition1, createContext(request));
    assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();
  }

  //

  @Test
  public void compareToMediaTypeAll() {
    MockRequest request = new MockRequest();

    ProducesRequestCondition condition1 = new ProducesRequestCondition();
    ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

    assertThat(condition1.compareTo(condition2, createContext(request)) < 0).as("Should have picked '*/*' condition as an exact match").isTrue();
    assertThat(condition2.compareTo(condition1, createContext(request)) > 0).as("Should have picked '*/*' condition as an exact match").isTrue();

    condition1 = new ProducesRequestCondition("*/*");
    condition2 = new ProducesRequestCondition("application/json");

    assertThat(condition1.compareTo(condition2, createContext(request)) < 0).isTrue();
    assertThat(condition2.compareTo(condition1, createContext(request)) > 0).isTrue();

    request.addHeader("Accept", "*/*");

    condition1 = new ProducesRequestCondition();
    condition2 = new ProducesRequestCondition("application/json");

    assertThat(condition1.compareTo(condition2, createContext(request)) < 0).isTrue();
    assertThat(condition2.compareTo(condition1, createContext(request)) > 0).isTrue();

    condition1 = new ProducesRequestCondition("*/*");
    condition2 = new ProducesRequestCondition("application/json");

    assertThat(condition1.compareTo(condition2, createContext(request)) < 0).isTrue();
    assertThat(condition2.compareTo(condition1, createContext(request)) > 0).isTrue();
  }

  //

  @Test
  public void compareToMediaTypeAllWithParameter() {
    MockRequest request = createRequest("*/*;q=0.9");

    ProducesRequestCondition condition1 = new ProducesRequestCondition();
    ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

    assertThat(condition1.compareTo(condition2, createContext(request)) < 0).isTrue();
    assertThat(condition2.compareTo(condition1, createContext(request)) > 0).isTrue();
  }

  @Test
  public void compareToEqualMatch() {
    MockRequest request = createRequest("text/*");

    ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
    ProducesRequestCondition condition2 = new ProducesRequestCondition("text/xhtml");

    int result = condition1.compareTo(condition2, createContext(request));
    assertThat(result < 0).as("Should have used MediaType.equals(Object) to break the match").isTrue();

    result = condition2.compareTo(condition1, createContext(request));
    assertThat(result > 0).as("Should have used MediaType.equals(Object) to break the match").isTrue();
  }

  @Test
  public void combine() {
    ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
    ProducesRequestCondition condition2 = new ProducesRequestCondition("application/xml");

    ProducesRequestCondition result = condition1.combine(condition2);
    assertThat(result).isEqualTo(condition2);
  }

  @Test
  public void combineWithDefault() {
    ProducesRequestCondition condition1 = new ProducesRequestCondition("text/plain");
    ProducesRequestCondition condition2 = new ProducesRequestCondition();

    ProducesRequestCondition result = condition1.combine(condition2);
    assertThat(result).isEqualTo(condition1);
  }

  @Test
  public void instantiateWithProducesAndHeaderConditions() {
    String[] produces = new String[] { "text/plain" };
    String[] headers = new String[] { "foo=bar", "accept=application/xml,application/pdf" };
    ProducesRequestCondition condition = new ProducesRequestCondition(produces, headers);

    assertConditions(condition, "text/plain", "application/xml", "application/pdf");
  }

  @Test
  public void getMatchingCondition() {
    MockRequest request = createRequest("text/plain");

    ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

    ProducesRequestCondition result = condition.getMatchingCondition(createContext(request));
    assertConditions(result, "text/plain");

    condition = new ProducesRequestCondition("application/xml");

    result = condition.getMatchingCondition(createContext(request));
    assertThat(result).isNull();
  }

  private MockRequestContext createContext(MockRequest request) {
    return new MockRequestContext(null, request, null);
  }

  private MockRequest createRequest(String... headerValue) {
    MockRequest request = new MockRequest("GET", "/");
    Arrays.stream(headerValue).forEach(value -> request.addHeader("Accept", headerValue));
    return request;
  }

  private void assertConditions(ProducesRequestCondition condition, String... expected) {
    Collection<MediaTypeExpression> expressions = condition.getContent();
    assertThat(expressions.stream().map(expr -> expr.mediaType.toString()))
            .containsExactlyInAnyOrder(expected);
  }

}
