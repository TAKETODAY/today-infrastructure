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

package infra.test.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import infra.core.ParameterizedTypeReference;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * A helper class for applying assertions via JSON path expressions.
 *
 * <p>Based on the <a href="https://github.com/jayway/JsonPath">JsonPath</a>
 * project: requiring version 0.9+, with 1.1+ strongly recommended.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Craig Andrews
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JsonPathExpectationsHelper {

  private final String expression;

  private final JsonPath jsonPath;

  private final Configuration configuration;

  /**
   * Construct a new {@code JsonPathExpectationsHelper} using the
   * {@linkplain Configuration#defaultConfiguration() default configuration}.
   *
   * @param expression the {@link JsonPath} expression; never {@code null} or empty
   */
  public JsonPathExpectationsHelper(String expression) {
    this(expression, (Configuration) null);
  }

  /**
   * Construct a new {@code JsonPathExpectationsHelper}.
   *
   * @param expression the {@link JsonPath} expression; never {@code null} or empty
   * @param configuration the {@link Configuration} to use or {@code null} to use the
   * {@linkplain Configuration#defaultConfiguration() default configuration}
   */
  public JsonPathExpectationsHelper(String expression, @Nullable Configuration configuration) {
    Assert.hasText(expression, "expression must not be null or empty");
    this.expression = expression;
    this.jsonPath = JsonPath.compile(this.expression);
    this.configuration = (configuration != null) ? configuration : Configuration.defaultConfiguration();
  }

  /**
   * Construct a new {@code JsonPathExpectationsHelper}.
   *
   * @param expression the {@link JsonPath} expression; never {@code null} or empty
   * @param args arguments to parameterize the {@code JsonPath} expression with,
   * using formatting specifiers defined in {@link String#format(String, Object...)}
   */
  @Deprecated(forRemoval = true)
  public JsonPathExpectationsHelper(String expression, Object... args) {
    this(expression.formatted(args), (Configuration) null);
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert the resulting value with the given {@code Matcher}.
   *
   * @param content the JSON content
   * @param matcher the matcher with which to assert the result
   */
  @SuppressWarnings("unchecked")
  public <T> void assertValue(String content, Matcher<? super T> matcher) {
    T value = (T) evaluateJsonPath(content);
    MatcherAssert.assertThat("JSON path \"" + this.expression + "\"", value, matcher);
  }

  /**
   * An overloaded variant of {@link #assertValue(String, Matcher)} that also
   * accepts a target type for the resulting value. This can be useful for
   * matching numbers reliably for example coercing an integer into a double.
   *
   * @param content the JSON content
   * @param matcher the matcher with which to assert the result
   * @param targetType the expected type of the resulting value
   */
  public <T> void assertValue(String content, Matcher<? super T> matcher, Class<T> targetType) {
    T value = evaluateJsonPath(content, targetType);
    MatcherAssert.assertThat("JSON path \"" + this.expression + "\"", value, matcher);
  }

  /**
   * An overloaded variant of {@link #assertValue(String, Matcher)} that also
   * accepts a target type for the resulting value that allows generic types
   * to be defined.
   * <p>This must be used with a {@link Configuration} that defines a more
   * elaborate {@link MappingProvider} as the default one cannot handle
   * generic types.
   *
   * @param content the JSON content
   * @param matcher the matcher with which to assert the result
   * @param targetType the expected type of the resulting value
   */
  public <T> void assertValue(String content, Matcher<? super T> matcher, ParameterizedTypeReference<T> targetType) {
    T value = evaluateJsonPath(content, targetType);
    MatcherAssert.assertThat("JSON path \"" + this.expression + "\"", value, matcher);
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that the result is equal to the expected value.
   *
   * @param content the JSON content
   * @param expectedValue the expected value
   */
  public void assertValue(String content, @Nullable Object expectedValue) {
    Object actualValue = evaluateJsonPath(content);
    if ((actualValue instanceof List<?> actualValueList) && !(expectedValue instanceof List)) {
      if (actualValueList.isEmpty()) {
        AssertionErrors.fail("No matching value at JSON path \"" + this.expression + "\"");
      }
      if (actualValueList.size() != 1) {
        AssertionErrors.fail("Got a list of values " + actualValue +
                " instead of the expected single value " + expectedValue);
      }
      actualValue = actualValueList.get(0);
    }
    else if (actualValue != null && expectedValue != null &&
            !actualValue.getClass().equals(expectedValue.getClass())) {
      try {
        actualValue = evaluateJsonPath(content, expectedValue.getClass());
      }
      catch (AssertionError error) {
        String message = String.format(
                "At JSON path \"%s\", value <%s> of type <%s> cannot be converted to type <%s>",
                this.expression, actualValue, ClassUtils.getDescriptiveType(actualValue),
                ClassUtils.getDescriptiveType(expectedValue));
        throw new AssertionError(message, error.getCause());
      }
    }
    AssertionErrors.assertEquals("JSON path \"" + this.expression + "\"", expectedValue, actualValue);
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that the resulting value is a {@link String}.
   *
   * @param content the JSON content
   */
  public void assertValueIsString(String content) {
    Object value = assertExistsAndReturn(content);
    MatcherAssert.assertThat(failureReason("a string", value), value, CoreMatchers.instanceOf(String.class));
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that the resulting value is a {@link Boolean}.
   *
   * @param content the JSON content
   */
  public void assertValueIsBoolean(String content) {
    Object value = assertExistsAndReturn(content);
    MatcherAssert.assertThat(failureReason("a boolean", value), value, CoreMatchers.instanceOf(Boolean.class));
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that the resulting value is a {@link Number}.
   *
   * @param content the JSON content
   */
  public void assertValueIsNumber(String content) {
    Object value = assertExistsAndReturn(content);
    MatcherAssert.assertThat(failureReason("a number", value), value, CoreMatchers.instanceOf(Number.class));
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that the resulting value is an array.
   *
   * @param content the JSON content
   */
  public void assertValueIsArray(String content) {
    Object value = assertExistsAndReturn(content);
    MatcherAssert.assertThat(failureReason("an array", value), value, CoreMatchers.instanceOf(List.class));
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that the resulting value is a {@link Map}.
   *
   * @param content the JSON content
   */
  public void assertValueIsMap(String content) {
    Object value = assertExistsAndReturn(content);
    MatcherAssert.assertThat(failureReason("a map", value), value, CoreMatchers.instanceOf(Map.class));
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that a non-null value, possibly an empty array or map, exists
   * at the given path.
   * <p>Note that if the JSON path expression is not
   * {@linkplain JsonPath#isDefinite() definite}, this method asserts
   * that the list of values at the given path is not <em>empty</em>.
   *
   * @param content the JSON content
   */
  public void exists(String content) {
    assertExistsAndReturn(content);
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that a non-null value does not exist at the given path.
   * <p>Note that if the JSON path expression is not
   * {@linkplain JsonPath#isDefinite() definite}, this method asserts
   * that the list of values at the given path is <em>empty</em>.
   *
   * @param content the JSON content
   */
  public void doesNotExist(String content) {
    Object value;
    try {
      value = evaluateJsonPath(content);
    }
    catch (AssertionError ex) {
      return;
    }
    String reason = failureReason("no value", value);
    if (pathIsIndefinite() && value instanceof List<?> list) {
      AssertionErrors.assertTrue(reason, list.isEmpty());
    }
    else {
      AssertionErrors.assertTrue(reason, (value == null));
    }
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that an empty value exists at the given path.
   * <p>For the semantics of <em>empty</em>, consult the Javadoc for
   * {@link ObjectUtils#isEmpty(Object)}.
   *
   * @param content the JSON content
   */
  public void assertValueIsEmpty(String content) {
    Object value = evaluateJsonPath(content);
    AssertionErrors.assertTrue(failureReason("an empty value", value), ObjectUtils.isEmpty(value));
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that a non-empty value exists at the given path.
   * <p>For the semantics of <em>empty</em>, consult the Javadoc for
   * {@link ObjectUtils#isEmpty(Object)}.
   *
   * @param content the JSON content
   */
  public void assertValueIsNotEmpty(String content) {
    Object value = evaluateJsonPath(content);
    AssertionErrors.assertTrue(failureReason("a non-empty value", value), ObjectUtils.isNotEmpty(value));
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that a value, possibly {@code null}, exists.
   * <p>If the JSON path expression is not
   * {@linkplain JsonPath#isDefinite() definite}, this method asserts
   * that the list of values at the given path is not <em>empty</em>.
   *
   * @param content the JSON content
   */
  public void hasJsonPath(String content) {
    Object value = evaluateJsonPath(content);
    if (pathIsIndefinite() && value instanceof List<?> list) {
      String message = "No values for JSON path \"" + this.expression + "\"";
      AssertionErrors.assertTrue(message, !list.isEmpty());
    }
  }

  /**
   * Evaluate the JSON path expression against the supplied {@code content}
   * and assert that a value, including {@code null} values, does not exist
   * at the given path.
   * <p>If the JSON path expression is not
   * {@linkplain JsonPath#isDefinite() definite}, this method asserts
   * that the list of values at the given path is <em>empty</em>.
   *
   * @param content the JSON content
   */
  public void doesNotHaveJsonPath(String content) {
    Object value;
    try {
      value = evaluateJsonPath(content);
    }
    catch (AssertionError ex) {
      return;
    }
    if (pathIsIndefinite() && value instanceof List<?> list) {
      AssertionErrors.assertTrue(failureReason("no values", value), list.isEmpty());
    }
    else {
      AssertionErrors.fail(failureReason("no value", value));
    }
  }

  private String failureReason(String expectedDescription, @Nullable Object value) {
    return String.format("Expected %s at JSON path \"%s\" but found: %s", expectedDescription, this.expression,
            ObjectUtils.nullSafeToString(StringUtils.quoteIfString(value)));
  }

  /**
   * Evaluate the JSON path and return the resulting value.
   *
   * @param content the content to evaluate against
   * @return the result of the evaluation
   * @throws AssertionError if the evaluation fails
   */
  @Nullable
  public Object evaluateJsonPath(String content) {
    try {
      return this.jsonPath.read(content, this.configuration);
    }
    catch (Throwable ex) {
      throw new AssertionError("No value at JSON path \"" + this.expression + "\"", ex);
    }
  }

  /**
   * Variant of {@link #evaluateJsonPath(String)} with a target type.
   * <p>This can be useful for matching numbers reliably for example coercing an
   * integer into a double or when the configured {@link MappingProvider} can
   * handle more complex object structures.
   *
   * @param content the content to evaluate against
   * @param targetType the requested target type
   * @return the result of the evaluation
   * @throws AssertionError if the evaluation fails
   */
  public <T> T evaluateJsonPath(String content, Class<T> targetType) {
    return evaluateExpression(content, context -> context.read(this.expression, targetType));
  }

  /**
   * Variant of {@link #evaluateJsonPath(String)} with a target type that has
   * generics.
   * <p>This must be used with a {@link Configuration} that defines a more
   * elaborate {@link MappingProvider} as the default one cannot handle
   * generic types.
   *
   * @param content the content to evaluate against
   * @param targetType the requested target type
   * @return the result of the evaluation
   * @throws AssertionError if the evaluation fails
   */
  public <T> T evaluateJsonPath(String content, ParameterizedTypeReference<T> targetType) {
    return evaluateExpression(content, context ->
            context.read(this.expression, new TypeRefAdapter<>(targetType)));
  }

  @Nullable
  private Object assertExistsAndReturn(String content) {
    Object value = evaluateJsonPath(content);
    String reason = "No value at JSON path \"" + this.expression + "\"";
    AssertionErrors.assertTrue(reason, value != null);
    if (pathIsIndefinite() && value instanceof List<?> list) {
      AssertionErrors.assertTrue(reason, !list.isEmpty());
    }
    return value;
  }

  private boolean pathIsIndefinite() {
    return !this.jsonPath.isDefinite();
  }

  private <T> T evaluateExpression(String content, Function<DocumentContext, T> action) {
    try {
      DocumentContext context = JsonPath.parse(content, this.configuration);
      return action.apply(context);
    }
    catch (Throwable ex) {
      String message = "Failed to evaluate JSON path \"" + this.expression + "\"";
      throw new AssertionError(message, ex);
    }
  }

  /**
   * Adapt JSONPath {@link TypeRef} to {@link ParameterizedTypeReference}.
   */
  private static final class TypeRefAdapter<T> extends TypeRef<T> {

    private final Type type;

    TypeRefAdapter(ParameterizedTypeReference<T> typeReference) {
      this.type = typeReference.getType();
    }

    @Override
    public Type getType() {
      return this.type;
    }
  }

}
