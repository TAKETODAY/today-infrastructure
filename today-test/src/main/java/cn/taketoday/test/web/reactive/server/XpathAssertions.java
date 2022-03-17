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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.xpath.XPathExpressionException;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.util.XpathExpectationsHelper;
import cn.taketoday.util.MimeType;

/**
 * XPath assertions for the {@link WebTestClient}.
 *
 * @author Eric Deandrea
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class XpathAssertions {

  private final WebTestClient.BodyContentSpec bodySpec;

  private final XpathExpectationsHelper xpathHelper;

  XpathAssertions(WebTestClient.BodyContentSpec spec,
          String expression, @Nullable Map<String, String> namespaces, Object... args) {

    this.bodySpec = spec;
    this.xpathHelper = initXpathHelper(expression, namespaces, args);
  }

  private static XpathExpectationsHelper initXpathHelper(
          String expression, @Nullable Map<String, String> namespaces, Object[] args) {

    try {
      return new XpathExpectationsHelper(expression, namespaces, args);
    }
    catch (XPathExpressionException ex) {
      throw new AssertionError("XML parsing error", ex);
    }
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#assertString(byte[], String, String)}.
   */
  public WebTestClient.BodyContentSpec isEqualTo(String expectedValue) {
    return assertWith(() -> this.xpathHelper.assertString(getContent(), getCharset(), expectedValue));
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#assertNumber(byte[], String, Double)}.
   */
  public WebTestClient.BodyContentSpec isEqualTo(Double expectedValue) {
    return assertWith(() -> this.xpathHelper.assertNumber(getContent(), getCharset(), expectedValue));
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#assertBoolean(byte[], String, boolean)}.
   */
  public WebTestClient.BodyContentSpec isEqualTo(boolean expectedValue) {
    return assertWith(() -> this.xpathHelper.assertBoolean(getContent(), getCharset(), expectedValue));
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#exists(byte[], String)}.
   */
  public WebTestClient.BodyContentSpec exists() {
    return assertWith(() -> this.xpathHelper.exists(getContent(), getCharset()));
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#doesNotExist(byte[], String)}.
   */
  public WebTestClient.BodyContentSpec doesNotExist() {
    return assertWith(() -> this.xpathHelper.doesNotExist(getContent(), getCharset()));
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#assertNodeCount(byte[], String, int)}.
   */
  public WebTestClient.BodyContentSpec nodeCount(int expectedCount) {
    return assertWith(() -> this.xpathHelper.assertNodeCount(getContent(), getCharset(), expectedCount));
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#assertString(byte[], String, Matcher)}.
   *
   * @since 4.0
   */
  public WebTestClient.BodyContentSpec string(Matcher<? super String> matcher) {
    return assertWith(() -> this.xpathHelper.assertString(getContent(), getCharset(), matcher));
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#assertNumber(byte[], String, Matcher)}.
   *
   * @since 4.0
   */
  public WebTestClient.BodyContentSpec number(Matcher<? super Double> matcher) {
    return assertWith(() -> this.xpathHelper.assertNumber(getContent(), getCharset(), matcher));
  }

  /**
   * Delegates to {@link XpathExpectationsHelper#assertNodeCount(byte[], String, Matcher)}.
   *
   * @since 4.0
   */
  public WebTestClient.BodyContentSpec nodeCount(Matcher<? super Integer> matcher) {
    return assertWith(() -> this.xpathHelper.assertNodeCount(getContent(), getCharset(), matcher));
  }

  /**
   * Consume the result of the XPath evaluation as a String.
   *
   * @since 4.0
   */
  public WebTestClient.BodyContentSpec string(Consumer<String> consumer) {
    return assertWith(() -> {
      String value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), String.class);
      consumer.accept(value);
    });
  }

  /**
   * Consume the result of the XPath evaluation as a Double.
   *
   * @since 4.0
   */
  public WebTestClient.BodyContentSpec number(Consumer<Double> consumer) {
    return assertWith(() -> {
      Double value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), Double.class);
      consumer.accept(value);
    });
  }

  /**
   * Consume the count of nodes as result of the XPath evaluation.
   *
   * @since 4.0
   */
  public WebTestClient.BodyContentSpec nodeCount(Consumer<Integer> consumer) {
    return assertWith(() -> {
      Integer value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), Integer.class);
      consumer.accept(value);
    });
  }

  private WebTestClient.BodyContentSpec assertWith(CheckedExceptionTask task) {
    try {
      task.run();
    }
    catch (Exception ex) {
      throw new AssertionError("XML parsing error", ex);
    }
    return this.bodySpec;
  }

  private byte[] getContent() {
    byte[] body = this.bodySpec.returnResult().getResponseBody();
    Assert.notNull(body, "Expected body content");
    return body;
  }

  private String getCharset() {
    return Optional.of(this.bodySpec.returnResult())
            .map(EntityExchangeResult::getResponseHeaders)
            .map(HttpHeaders::getContentType)
            .map(MimeType::getCharset)
            .orElse(StandardCharsets.UTF_8)
            .name();
  }

  @Override
  public boolean equals(Object obj) {
    throw new AssertionError("Object#equals is disabled " +
            "to avoid being used in error instead of XPathAssertions#isEqualTo(String).");
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Lets us be able to use lambda expressions that could throw checked exceptions, since
   * {@link XpathExpectationsHelper} throws {@link Exception} on its methods.
   */
  private interface CheckedExceptionTask {

    void run() throws Exception;

  }
}
