/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.web.client.match;

import org.hamcrest.Matcher;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.test.web.client.RequestMatcher;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.util.UriComponentsBuilder;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.fail;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Static factory methods for {@link RequestMatcher} classes. Typically used to
 * provide input for {@link MockRestServiceServer#expect(RequestMatcher)}.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class MockRestRequestMatchers {

  /**
   * Match to any request.
   */
  public static RequestMatcher anything() {
    return request -> { };
  }

  /**
   * Assert the {@link HttpMethod} of the request.
   *
   * @param method the HTTP method
   * @return the request matcher
   */
  public static RequestMatcher method(HttpMethod method) {
    Assert.notNull(method, "'method' is required");
    return request -> assertEquals("Unexpected HttpMethod", method, request.getMethod());
  }

  /**
   * Assert the request URI string with the given Hamcrest matcher.
   *
   * @param matcher the String matcher for the expected URI
   * @return the request matcher
   */
  public static RequestMatcher requestTo(Matcher<? super String> matcher) {
    Assert.notNull(matcher, "'matcher' is required");
    return request -> assertThat("Request URI", request.getURI().toString(), matcher);
  }

  /**
   * Assert the request URI matches the given string.
   *
   * @param expectedUri the expected URI
   * @return the request matcher
   */
  public static RequestMatcher requestTo(String expectedUri) {
    Assert.notNull(expectedUri, "'uri' is required");
    return request -> assertEquals("Request URI", expectedUri, request.getURI().toString());
  }

  /**
   * Variant of {@link #requestTo(URI)} that prepares the URI from a URI
   * template plus optional variables via {@link UriComponentsBuilder}
   * including encoding.
   *
   * @param expectedUri the expected URI template
   * @param uriVars zero or more URI variables to populate the expected URI
   * @return the request matcher
   */
  public static RequestMatcher requestToUriTemplate(String expectedUri, Object... uriVars) {
    Assert.notNull(expectedUri, "'uri' is required");
    URI uri = UriComponentsBuilder.fromUriString(expectedUri).buildAndExpand(uriVars).encode().toUri();
    return requestTo(uri);
  }

  /**
   * Expect a request to the given URI.
   *
   * @param uri the expected URI
   * @return the request matcher
   */
  public static RequestMatcher requestTo(URI uri) {
    Assert.notNull(uri, "'uri' is required");
    return request -> assertEquals("Unexpected request", uri, request.getURI());
  }

  /**
   * Assert request query parameter values with the given Hamcrest matcher(s).
   */
  @SafeVarargs
  public static RequestMatcher queryParam(String name, Matcher<? super String>... matchers) {
    return request -> {
      MultiValueMap<String, String> params = getQueryParams(request);
      assertValueCount("query param", name, params, matchers.length);
      for (int i = 0; i < matchers.length; i++) {
        assertThat("Query param", params.get(name).get(i), matchers[i]);
      }
    };
  }

  /**
   * Assert request query parameter values.
   */
  public static RequestMatcher queryParam(String name, String... expectedValues) {
    return request -> {
      MultiValueMap<String, String> params = getQueryParams(request);
      assertValueCount("query param", name, params, expectedValues.length);
      for (int i = 0; i < expectedValues.length; i++) {
        assertEquals("Query param [" + name + "]", expectedValues[i], params.get(name).get(i));
      }
    };
  }

  private static MultiValueMap<String, String> getQueryParams(ClientHttpRequest request) {
    return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
  }

  private static void assertValueCount(
          String valueType, String name, MultiValueMap<String, String> map, int count) {

    List<String> values = map.get(name);
    String message = "Expected " + valueType + " <" + name + ">";
    if (values == null) {
      fail(message + " to exist but was null");
    }
    if (count > values.size()) {
      fail(message + " to have at least <" + count + "> values but found " + values);
    }
  }

  /**
   * Assert request header values with the given Hamcrest matcher(s).
   */
  @SafeVarargs
  public static RequestMatcher header(String name, Matcher<? super String>... matchers) {
    return request -> {
      assertValueCount("header", name, request.getHeaders(), matchers.length);
      List<String> headerValues = request.getHeaders().get(name);
      Assert.state(headerValues != null, "No header values");
      for (int i = 0; i < matchers.length; i++) {
        assertThat("Request header [" + name + "]", headerValues.get(i), matchers[i]);
      }
    };
  }

  /**
   * Assert request header values.
   */
  public static RequestMatcher header(String name, String... expectedValues) {
    return request -> {
      assertValueCount("header", name, request.getHeaders(), expectedValues.length);
      List<String> headerValues = request.getHeaders().get(name);
      Assert.state(headerValues != null, "No header values");
      for (int i = 0; i < expectedValues.length; i++) {
        assertEquals("Request header [" + name + "]", expectedValues[i], headerValues.get(i));
      }
    };
  }

  /**
   * Assert that the given request header does not exist.
   *
   * @since 4.0
   */
  public static RequestMatcher headerDoesNotExist(String name) {
    return request -> {
      List<String> headerValues = request.getHeaders().get(name);
      if (headerValues != null) {
        fail("Expected header <" + name + "> not to exist, but it exists with values: " +
                headerValues);
      }
    };
  }

  /**
   * Access to request body matchers.
   */
  public static ContentRequestMatchers content() {
    return new ContentRequestMatchers();
  }

  /**
   * Access to request body matchers using a
   * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression to
   * inspect a specific subset of the body. The JSON path expression can be a
   * parameterized string using formatting specifiers as defined in
   * {@link String#format(String, Object...)}.
   *
   * @param expression the JSON path optionally parameterized with arguments
   * @param args arguments to parameterize the JSON path expression with
   */
  public static JsonPathRequestMatchers jsonPath(String expression, Object... args) {
    return new JsonPathRequestMatchers(expression, args);
  }

  /**
   * Access to request body matchers using a
   * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression to
   * inspect a specific subset of the body and a Hamcrest match for asserting
   * the value found at the JSON path.
   *
   * @param expression the JSON path expression
   * @param matcher a matcher for the value expected at the JSON path
   */
  public static <T> RequestMatcher jsonPath(String expression, Matcher<? super T> matcher) {
    return new JsonPathRequestMatchers(expression).value(matcher);
  }

  /**
   * Access to request body matchers using an XPath to inspect a specific
   * subset of the body. The XPath expression can be a parameterized string
   * using formatting specifiers as defined in
   * {@link String#format(String, Object...)}.
   *
   * @param expression the XPath optionally parameterized with arguments
   * @param args arguments to parameterize the XPath expression with
   */
  public static XpathRequestMatchers xpath(String expression, Object... args) throws XPathExpressionException {
    return new XpathRequestMatchers(expression, null, args);
  }

  /**
   * Access to response body matchers using an XPath to inspect a specific
   * subset of the body. The XPath expression can be a parameterized string
   * using formatting specifiers as defined in
   * {@link String#format(String, Object...)}.
   *
   * @param expression the XPath optionally parameterized with arguments
   * @param namespaces the namespaces referenced in the XPath expression
   * @param args arguments to parameterize the XPath expression with
   */
  public static XpathRequestMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
          throws XPathExpressionException {

    return new XpathRequestMatchers(expression, namespaces, args);
  }

}
