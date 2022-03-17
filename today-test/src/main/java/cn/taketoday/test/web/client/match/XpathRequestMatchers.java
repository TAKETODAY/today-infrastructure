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

package cn.taketoday.test.web.client.match;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.http.client.MockClientHttpRequest;
import cn.taketoday.test.util.XpathExpectationsHelper;
import cn.taketoday.test.web.client.RequestMatcher;

/**
 * Factory methods for request content {@code RequestMatcher} implementations
 * that use an XPath expression.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockRestRequestMatchers#xpath(String, Object...)} or
 * {@link MockRestRequestMatchers#xpath(String, Map, Object...)}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class XpathRequestMatchers {

  private static final String DEFAULT_ENCODING = "UTF-8";

  private final XpathExpectationsHelper xpathHelper;

  /**
   * Class constructor, not for direct instantiation.
   * <p>Use {@link MockRestRequestMatchers#xpath(String, Object...)} or
   * {@link MockRestRequestMatchers#xpath(String, Map, Object...)}.
   *
   * @param expression the XPath expression
   * @param namespaces the XML namespaces referenced in the XPath expression, or {@code null}
   * @param args arguments to parameterize the XPath expression with, using the
   * formatting specifiers defined in {@link String#format(String, Object...)}
   * @throws XPathExpressionException if expression compilation failed
   */
  protected XpathRequestMatchers(String expression, @Nullable Map<String, String> namespaces, Object... args)
          throws XPathExpressionException {

    this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
  }

  /**
   * Apply the XPath and assert it with the given {@code Matcher<Node>}.
   */
  public RequestMatcher node(Matcher<? super Node> matcher) {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.assertNode(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
  }

  /**
   * Assert that content exists at the given XPath.
   */
  public RequestMatcher exists() {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.exists(request.getBodyAsBytes(), DEFAULT_ENCODING);
  }

  /**
   * Assert that content does not exist at the given XPath.
   */
  public RequestMatcher doesNotExist() {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.doesNotExist(request.getBodyAsBytes(), DEFAULT_ENCODING);
  }

  /**
   * Apply the XPath and assert the number of nodes found with the given
   * {@code Matcher<Integer>}.
   */
  public RequestMatcher nodeCount(Matcher<? super Integer> matcher) {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
  }

  /**
   * Apply the XPath and assert the number of nodes found.
   */
  public RequestMatcher nodeCount(int expectedCount) {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, expectedCount);
  }

  /**
   * Apply the XPath and assert the String content found with the given matcher.
   */
  public RequestMatcher string(Matcher<? super String> matcher) {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
  }

  /**
   * Apply the XPath and assert the String content found.
   */
  public RequestMatcher string(String content) {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, content);
  }

  /**
   * Apply the XPath and assert the number found with the given matcher.
   */
  public RequestMatcher number(Matcher<? super Double> matcher) {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
  }

  /**
   * Apply the XPath and assert the number value found.
   */
  public RequestMatcher number(Double value) {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
  }

  /**
   * Apply the XPath and assert the boolean value found.
   */
  public RequestMatcher booleanValue(Boolean value) {
    return (XpathRequestMatcher) request ->
            this.xpathHelper.assertBoolean(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
  }

  /**
   * Functional interface for XPath {@link RequestMatcher} implementations.
   */
  @FunctionalInterface
  private interface XpathRequestMatcher extends RequestMatcher {

    @Override
    default void match(ClientHttpRequest request) {
      try {
        matchInternal((MockClientHttpRequest) request);
      }
      catch (Exception ex) {
        throw new AssertionError("Failed to parse XML request content", ex);
      }
    }

    void matchInternal(MockClientHttpRequest request) throws Exception;
  }

}
