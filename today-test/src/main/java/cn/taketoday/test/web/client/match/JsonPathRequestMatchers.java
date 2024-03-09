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

package cn.taketoday.test.web.client.match;

import com.jayway.jsonpath.JsonPath;

import org.hamcrest.Matcher;

import java.io.IOException;
import java.text.ParseException;

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.mock.http.client.MockClientHttpRequest;
import cn.taketoday.test.util.JsonPathExpectationsHelper;
import cn.taketoday.test.web.client.RequestMatcher;

/**
 * Factory for assertions on the request content using
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expressions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockRestRequestMatchers#jsonPath(String, Matcher)} or
 * {@link MockRestRequestMatchers#jsonPath(String, Object...)}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JsonPathRequestMatchers {

  private final JsonPathExpectationsHelper jsonPathHelper;

  /**
   * Protected constructor.
   * <p>Use {@link MockRestRequestMatchers#jsonPath(String, Matcher)} or
   * {@link MockRestRequestMatchers#jsonPath(String, Object...)}.
   *
   * @param expression the {@link JsonPath} expression; never {@code null} or empty
   * @param args arguments to parameterize the {@code JsonPath} expression with,
   * using formatting specifiers defined in {@link String#format(String, Object...)}
   */
  protected JsonPathRequestMatchers(String expression, Object... args) {
    Assert.hasText(expression, "expression must not be null or empty");
    this.jsonPathHelper = new JsonPathExpectationsHelper(expression.formatted(args));
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert the resulting value with the given Hamcrest {@link Matcher}.
   */
  public <T> RequestMatcher value(Matcher<? super T> matcher) {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValue(request.getBodyAsString(), matcher);
      }
    };
  }

  /**
   * An overloaded variant of {@link #value(Matcher)} that also accepts a
   * target type for the resulting value that the matcher can work reliably
   * against.
   * <p>This can be useful for matching numbers reliably &mdash; for example,
   * to coerce an integer into a double.
   */
  public <T> RequestMatcher value(Matcher<? super T> matcher, Class<T> targetType) {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        String body = request.getBodyAsString();
        JsonPathRequestMatchers.this.jsonPathHelper.assertValue(body, matcher, targetType);
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that the result is equal to the supplied value.
   */
  public RequestMatcher value(Object expectedValue) {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValue(request.getBodyAsString(), expectedValue);
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that a non-null value exists at the given path.
   * <p>If the JSON path expression is not {@linkplain JsonPath#isDefinite
   * definite}, this method asserts that the value at the given path is not
   * <em>empty</em>.
   */
  public RequestMatcher exists() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.exists(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that a value does not exist at the given path.
   * <p>If the JSON path expression is not {@linkplain JsonPath#isDefinite
   * definite}, this method asserts that the value at the given path is
   * <em>empty</em>.
   */
  public RequestMatcher doesNotExist() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.doesNotExist(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the response content
   * and assert that a value, possibly {@code null}, exists.
   * <p>If the JSON path expression is not
   * {@linkplain JsonPath#isDefinite() definite}, this method asserts
   * that the list of values at the given path is not <em>empty</em>.
   *
   * @see #exists()
   * @see #isNotEmpty()
   */
  public RequestMatcher hasJsonPath() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) {
        JsonPathRequestMatchers.this.jsonPathHelper.hasJsonPath(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that a value, including {@code null} values, does not exist
   * at the given path.
   * <p>If the JSON path expression is not
   * {@linkplain JsonPath#isDefinite() definite}, this method asserts
   * that the list of values at the given path is <em>empty</em>.
   *
   * @see #doesNotExist()
   * @see #isEmpty()
   */
  public RequestMatcher doesNotHaveJsonPath() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) {
        JsonPathRequestMatchers.this.jsonPathHelper.doesNotHaveJsonPath(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that an empty value exists at the given path.
   * <p>For the semantics of <em>empty</em>, consult the Javadoc for
   * {@link cn.taketoday.util.ObjectUtils#isEmpty(Object)}.
   *
   * @see #isNotEmpty()
   * @see #exists()
   * @see #doesNotExist()
   */
  public RequestMatcher isEmpty() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsEmpty(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that a non-empty value exists at the given path.
   * <p>For the semantics of <em>empty</em>, consult the Javadoc for
   * {@link cn.taketoday.util.ObjectUtils#isEmpty(Object)}.
   *
   * @see #isEmpty()
   * @see #exists()
   * @see #doesNotExist()
   */
  public RequestMatcher isNotEmpty() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsNotEmpty(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that the result is a {@link String}.
   */
  public RequestMatcher isString() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsString(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that the result is a {@link Boolean}.
   */
  public RequestMatcher isBoolean() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsBoolean(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that the result is a {@link Number}.
   */
  public RequestMatcher isNumber() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsNumber(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that the result is an array.
   */
  public RequestMatcher isArray() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsArray(request.getBodyAsString());
      }
    };
  }

  /**
   * Evaluate the JSON path expression against the request content and
   * assert that the result is a {@link java.util.Map}.
   */
  public RequestMatcher isMap() {
    return new AbstractJsonPathRequestMatcher() {
      @Override
      public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
        JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsMap(request.getBodyAsString());
      }
    };
  }

  /**
   * Abstract base class for {@code JsonPath}-based {@link RequestMatcher RequestMatchers}.
   *
   * @see #matchInternal
   */
  private abstract static class AbstractJsonPathRequestMatcher implements RequestMatcher {

    @Override
    public final void match(ClientHttpRequest request) throws IOException, AssertionError {
      try {
        MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
        matchInternal(mockRequest);
      }
      catch (ParseException ex) {
        throw new AssertionError("Failed to parse JSON request content", ex);
      }
    }

    abstract void matchInternal(MockClientHttpRequest request) throws IOException, ParseException;
  }

}
