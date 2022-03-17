/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.test.web.reactive.server;

import org.hamcrest.Matcher;

import java.util.function.Consumer;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.util.JsonPathExpectationsHelper;

/**
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> assertions.
 *
 * @author Rossen Stoyanchev
 * @see <a href="https://github.com/jayway/JsonPath">https://github.com/jayway/JsonPath</a>
 * @see JsonPathExpectationsHelper
 * @since 4.0
 */
public class JsonPathAssertions {

  private final WebTestClient.BodyContentSpec bodySpec;

  private final String content;

  private final JsonPathExpectationsHelper pathHelper;

  JsonPathAssertions(WebTestClient.BodyContentSpec spec, String content, String expression, Object... args) {
    this.bodySpec = spec;
    this.content = content;
    this.pathHelper = new JsonPathExpectationsHelper(expression, args);
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#assertValue(String, Object)}.
   */
  public WebTestClient.BodyContentSpec isEqualTo(Object expectedValue) {
    this.pathHelper.assertValue(this.content, expectedValue);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#exists(String)}.
   */
  public WebTestClient.BodyContentSpec exists() {
    this.pathHelper.exists(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#doesNotExist(String)}.
   */
  public WebTestClient.BodyContentSpec doesNotExist() {
    this.pathHelper.doesNotExist(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#assertValueIsEmpty(String)}.
   */
  public WebTestClient.BodyContentSpec isEmpty() {
    this.pathHelper.assertValueIsEmpty(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#assertValueIsNotEmpty(String)}.
   */
  public WebTestClient.BodyContentSpec isNotEmpty() {
    this.pathHelper.assertValueIsNotEmpty(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#hasJsonPath}.
   *
   * @since 4.0
   */
  public WebTestClient.BodyContentSpec hasJsonPath() {
    this.pathHelper.hasJsonPath(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#doesNotHaveJsonPath}.
   *
   * @since 4.0
   */
  public WebTestClient.BodyContentSpec doesNotHaveJsonPath() {
    this.pathHelper.doesNotHaveJsonPath(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#assertValueIsBoolean(String)}.
   */
  public WebTestClient.BodyContentSpec isBoolean() {
    this.pathHelper.assertValueIsBoolean(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#assertValueIsNumber(String)}.
   */
  public WebTestClient.BodyContentSpec isNumber() {
    this.pathHelper.assertValueIsNumber(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#assertValueIsArray(String)}.
   */
  public WebTestClient.BodyContentSpec isArray() {
    this.pathHelper.assertValueIsArray(this.content);
    return this.bodySpec;
  }

  /**
   * Applies {@link JsonPathExpectationsHelper#assertValueIsMap(String)}.
   */
  public WebTestClient.BodyContentSpec isMap() {
    this.pathHelper.assertValueIsMap(this.content);
    return this.bodySpec;
  }

  /**
   * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher)}.
   *
   * @since 4.0
   */
  public <T> WebTestClient.BodyContentSpec value(Matcher<? super T> matcher) {
    this.pathHelper.assertValue(this.content, matcher);
    return this.bodySpec;
  }

  /**
   * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, Class)}.
   *
   * @since 4.0
   */
  public <T> WebTestClient.BodyContentSpec value(Matcher<? super T> matcher, Class<T> targetType) {
    this.pathHelper.assertValue(this.content, matcher, targetType);
    return this.bodySpec;
  }

  /**
   * Consume the result of the JSONPath evaluation.
   *
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public <T> WebTestClient.BodyContentSpec value(Consumer<T> consumer) {
    Object value = this.pathHelper.evaluateJsonPath(this.content);
    consumer.accept((T) value);
    return this.bodySpec;
  }

  /**
   * Consume the result of the JSONPath evaluation and provide a target class.
   *
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public <T> WebTestClient.BodyContentSpec value(Consumer<T> consumer, Class<T> targetType) {
    Object value = this.pathHelper.evaluateJsonPath(this.content, targetType);
    consumer.accept((T) value);
    return this.bodySpec;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    throw new AssertionError("Object#equals is disabled " +
            "to avoid being used in error instead of JsonPathAssertions#isEqualTo(String).");
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
