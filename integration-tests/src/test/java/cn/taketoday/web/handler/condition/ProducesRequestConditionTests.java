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

import java.util.Arrays;
import java.util.Collection;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.NonNull;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.FixedContentNegotiationStrategy;
import cn.taketoday.web.accept.HeaderContentNegotiationStrategy;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.mock.api.http.HttpMockRequest;

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
    HttpMockRequest request = createRequest("text/plain");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test
  public void matchNegated() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");
    HttpMockRequest request = createRequest("text/plain");

    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void matchNegatedWithoutAcceptHeader() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");

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
    HttpMockRequest request = createRequest("text/plain");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test
  public void matchMultiple() {
    ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");
    HttpMockRequest request = createRequest("text/plain");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test
  public void matchSingle() {
    ProducesRequestCondition condition = new ProducesRequestCondition("text/plain");
    HttpMockRequest request = createRequest("application/xml");

    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

  @Test // gh-21670
  public void matchWithParameters() {
    String base = "application/atom+xml";
    ProducesRequestCondition condition = new ProducesRequestCondition(base + ";type=feed");
    HttpMockRequest request = createRequest(base + ";type=entry");
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
    HttpMockRequest request = createRequest("bogus");

    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void matchParseErrorWithNegation() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!text/plain");
    HttpMockRequest request = createRequest("bogus");

    assertThat(condition.getMatchingCondition(createContext(request))).isNull();
  }

  @Test
  public void matchByRequestParameter() {
    String[] produces = { "text/plain" };
    String[] headers = {};
    ProducesRequestCondition condition = new ProducesRequestCondition(produces, headers);
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/foo.txt");

    assertThat(condition.getMatchingCondition(createContext(request))).isNotNull();
  }

  @Test // SPR-17550
  public void matchWithNegationAndMediaTypeAllWithQualityParameter() {
    ProducesRequestCondition condition = new ProducesRequestCondition("!application/json");
    HttpMockRequest request = createRequest(
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

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
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

    HttpMockRequest request = createRequest("application/xml, text/html");

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

    // See SPR-7000
    request = createRequest("text/html;q=0.9,application/xml");

    assertThat(html.compareTo(xml, createContext(request)) > 0).isTrue();
    assertThat(xml.compareTo(html, createContext(request)) < 0).isTrue();
  }

  @Test
  public void compareToWithSingleExpression() {
    HttpMockRequest request = createRequest("text/plain");

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

    HttpMockRequest request = createRequest("text/plain");

    int result = condition1.compareTo(condition2, createContext(request));
    assertThat(result).as("Invalid comparison result: " + result).isGreaterThan(0);

    result = condition2.compareTo(condition1, createContext(request));
    assertThat(result).as("Invalid comparison result: " + result).isLessThan(0);
  }

  @Test
  public void compareToMultipleExpressionsAndMultipleAcceptHeaderValues() {
    ProducesRequestCondition condition1 = new ProducesRequestCondition("text/*", "text/plain");
    ProducesRequestCondition condition2 = new ProducesRequestCondition("application/*", "application/xml");

    HttpMockRequest request = createRequest("text/plain", "application/xml");

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

  // SPR-8536

  @Test
  public void compareToMediaTypeAll() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();

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

  // SPR-9021

  @Test
  public void compareToMediaTypeAllWithParameter() {
    HttpMockRequest request = createRequest("*/*;q=0.9");

    ProducesRequestCondition condition1 = new ProducesRequestCondition();
    ProducesRequestCondition condition2 = new ProducesRequestCondition("application/json");

    assertThat(condition1.compareTo(condition2, createContext(request)) < 0).isTrue();
    assertThat(condition2.compareTo(condition1, createContext(request)) > 0).isTrue();
  }

  @Test
  public void compareToEqualMatch() {
    HttpMockRequest request = createRequest("text/*");

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
    HttpMockRequest request = createRequest("text/plain");

    ProducesRequestCondition condition = new ProducesRequestCondition("text/plain", "application/xml");

    ProducesRequestCondition result = condition.getMatchingCondition(createContext(request));
    assertConditions(result, "text/plain");

    condition = new ProducesRequestCondition("application/xml");

    result = condition.getMatchingCondition(createContext(request));
    assertThat(result).isNull();
  }

  @NonNull
  private ServletRequestContext createContext(HttpMockRequest request) {
    return new ServletRequestContext(null, request, null);
  }

  private HttpMockRequestImpl createRequest(String... headerValue) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    Arrays.stream(headerValue).forEach(value -> request.addHeader("Accept", headerValue));
    return request;
  }

  private void assertConditions(ProducesRequestCondition condition, String... expected) {
    Collection<MediaTypeExpression> expressions = condition.getContent();
    assertThat(expressions.stream().map(expr -> expr.mediaType.toString()))
            .containsExactlyInAnyOrder(expected);
  }

}
