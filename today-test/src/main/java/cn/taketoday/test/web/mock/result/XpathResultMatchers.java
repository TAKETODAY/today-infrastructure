/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.mock.result;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.test.util.XpathExpectationsHelper;
import cn.taketoday.test.web.mock.ResultMatcher;

/**
 * Factory for assertions on the response content using XPath expressions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#xpath}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class XpathResultMatchers {

  private final XpathExpectationsHelper xpathHelper;

  /**
   * Protected constructor, not for direct instantiation. Use
   * {@link MockMvcResultMatchers#xpath(String, Object...)} or
   * {@link MockMvcResultMatchers#xpath(String, Map, Object...)}.
   *
   * @param expression the XPath expression
   * @param namespaces the XML namespaces referenced in the XPath expression, or {@code null}
   * @param args arguments to parameterize the XPath expression with using the
   * formatting specifiers defined in {@link String#format(String, Object...)}
   */
  protected XpathResultMatchers(String expression, @Nullable Map<String, String> namespaces, Object... args)
          throws XPathExpressionException {

    this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
  }

  /**
   * Evaluate the XPath and assert the {@link Node} content found with the
   * given Hamcrest {@link Matcher}.
   */
  public ResultMatcher node(Matcher<? super Node> matcher) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertNode(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
    };
  }

  /**
   * Evaluate the XPath and assert the {@link NodeList} content found with the
   * given Hamcrest {@link Matcher}.
   */
  public ResultMatcher nodeList(Matcher<? super NodeList> matcher) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertNodeList(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
    };
  }

  /**
   * Get the response encoding if explicitly defined in the response, {@code null} otherwise.
   */
  @Nullable
  private String getDefinedEncoding(MockHttpResponseImpl response) {
    return (response.isCharset() ? response.getCharacterEncoding() : null);
  }

  /**
   * Evaluate the XPath and assert that content exists.
   */
  public ResultMatcher exists() {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.exists(response.getContentAsByteArray(), getDefinedEncoding(response));
    };
  }

  /**
   * Evaluate the XPath and assert that content doesn't exist.
   */
  public ResultMatcher doesNotExist() {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.doesNotExist(response.getContentAsByteArray(), getDefinedEncoding(response));
    };
  }

  /**
   * Evaluate the XPath and assert the number of nodes found with the given
   * Hamcrest {@link Matcher}.
   */
  public ResultMatcher nodeCount(Matcher<? super Integer> matcher) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertNodeCount(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
    };
  }

  /**
   * Evaluate the XPath and assert the number of nodes found.
   */
  public ResultMatcher nodeCount(int expectedCount) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertNodeCount(response.getContentAsByteArray(), getDefinedEncoding(response), expectedCount);
    };
  }

  /**
   * Apply the XPath and assert the {@link String} value found with the given
   * Hamcrest {@link Matcher}.
   */
  public ResultMatcher string(Matcher<? super String> matcher) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertString(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
    };
  }

  /**
   * Apply the XPath and assert the {@link String} value found.
   */
  public ResultMatcher string(String expectedValue) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertString(response.getContentAsByteArray(), getDefinedEncoding(response), expectedValue);
    };
  }

  /**
   * Evaluate the XPath and assert the {@link Double} value found with the
   * given Hamcrest {@link Matcher}.
   */
  public ResultMatcher number(Matcher<? super Double> matcher) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertNumber(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
    };
  }

  /**
   * Evaluate the XPath and assert the {@link Double} value found.
   */
  public ResultMatcher number(Double expectedValue) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertNumber(response.getContentAsByteArray(), getDefinedEncoding(response), expectedValue);
    };
  }

  /**
   * Evaluate the XPath and assert the {@link Boolean} value found.
   */
  public ResultMatcher booleanValue(Boolean value) {
    return result -> {
      MockHttpResponseImpl response = result.getResponse();
      this.xpathHelper.assertBoolean(response.getContentAsByteArray(), getDefinedEncoding(response), value);
    };
  }

}
