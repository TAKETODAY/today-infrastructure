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

package cn.taketoday.test.web.servlet.result;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import cn.taketoday.http.MediaType;
import cn.taketoday.test.util.JsonExpectationsHelper;
import cn.taketoday.test.util.XmlExpectationsHelper;
import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.mock.api.http.HttpMockResponse;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.assertNotNull;
import static cn.taketoday.test.util.AssertionErrors.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for response content assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#content}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class ContentResultMatchers {

  private final XmlExpectationsHelper xmlHelper;

  private final JsonExpectationsHelper jsonHelper;

  /**
   * Protected constructor.
   * Use {@link MockMvcResultMatchers#content()}.
   */
  protected ContentResultMatchers() {
    this.xmlHelper = new XmlExpectationsHelper();
    this.jsonHelper = new JsonExpectationsHelper();
  }

  /**
   * Assert the ServletResponse content type. The given content type must
   * fully match including type, subtype, and parameters. For checking
   * only the type and subtype see {@link #contentTypeCompatibleWith(String)}.
   */
  public ResultMatcher contentType(String contentType) {
    return contentType(MediaType.parseMediaType(contentType));
  }

  /**
   * Assert the ServletResponse content type after parsing it as a MediaType.
   * The given content type must fully match including type, subtype, and
   * parameters. For checking only the type and subtype see
   * {@link #contentTypeCompatibleWith(MediaType)}.
   */
  public ResultMatcher contentType(MediaType contentType) {
    return result -> {
      String actual = result.getResponse().getContentType();
      assertNotNull("Content type not set", actual);
      assertEquals("Content type", contentType, MediaType.parseMediaType(actual));
    };
  }

  /**
   * Assert the ServletResponse content type is compatible with the given
   * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
   */
  public ResultMatcher contentTypeCompatibleWith(String contentType) {
    return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
  }

  /**
   * Assert the ServletResponse content type is compatible with the given
   * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
   */
  public ResultMatcher contentTypeCompatibleWith(MediaType contentType) {
    return result -> {
      String actual = result.getResponse().getContentType();
      assertNotNull("Content type not set", actual);
      MediaType actualContentType = MediaType.parseMediaType(actual);
      assertTrue("Content type [" + actual + "] is not compatible with [" + contentType + "]",
              actualContentType.isCompatibleWith(contentType));
    };
  }

  /**
   * Assert the character encoding in the ServletResponse.
   *
   * @see StandardCharsets
   * @see #encoding(String)
   */
  public ResultMatcher encoding(Charset characterEncoding) {
    return encoding(characterEncoding.name());
  }

  /**
   * Assert the character encoding in the ServletResponse.
   *
   * @see HttpMockResponse#getCharacterEncoding()
   */
  public ResultMatcher encoding(String characterEncoding) {
    return result -> {
      String actual = result.getResponse().getCharacterEncoding();
      assertEquals("Character encoding", characterEncoding, actual);
    };
  }

  /**
   * Assert the response body content with a Hamcrest {@link Matcher}.
   * <pre class="code">
   * mockMvc.perform(get("/path"))
   *   .andExpect(content().string(containsString("text")));
   * </pre>
   */
  public ResultMatcher string(Matcher<? super String> matcher) {
    return result -> assertThat("Response content", result.getResponse().getContentAsString(), matcher);
  }

  /**
   * Assert the response body content as a String.
   */
  public ResultMatcher string(String expectedContent) {
    return result -> assertEquals("Response content", expectedContent, result.getResponse().getContentAsString());
  }

  /**
   * Assert the response body content as a byte array.
   */
  public ResultMatcher bytes(byte[] expectedContent) {
    return result -> assertEquals("Response content", expectedContent, result.getResponse().getContentAsByteArray());
  }

  /**
   * Parse the response content and the given string as XML and assert the two
   * are "similar" - i.e. they contain the same elements and attributes
   * regardless of order.
   * <p>Use of this matcher requires the <a
   * href="https://www.xmlunit.org/">XMLUnit</a> library.
   *
   * @param xmlContent the expected XML content
   * @see MockMvcResultMatchers#xpath(String, Object...)
   * @see MockMvcResultMatchers#xpath(String, Map, Object...)
   */
  public ResultMatcher xml(String xmlContent) {
    return result -> {
      String content = result.getResponse().getContentAsString();
      this.xmlHelper.assertXmlEqual(xmlContent, content);
    };
  }

  /**
   * Parse the response content as {@link Node} and apply the given Hamcrest
   * {@link Matcher}.
   */
  public ResultMatcher node(Matcher<? super Node> matcher) {
    return result -> {
      String content = result.getResponse().getContentAsString();
      this.xmlHelper.assertNode(content, matcher);
    };
  }

  /**
   * Parse the response content as {@link DOMSource} and apply the given
   * Hamcrest {@link Matcher}.
   *
   * @see <a href="https://code.google.com/p/xml-matchers/">xml-matchers</a>
   */
  public ResultMatcher source(Matcher<? super Source> matcher) {
    return result -> {
      String content = result.getResponse().getContentAsString();
      this.xmlHelper.assertSource(content, matcher);
    };
  }

  /**
   * Parse the expected and actual strings as JSON and assert the two
   * are "similar" - i.e. they contain the same attribute-value pairs
   * regardless of formatting with a lenient checking (extensible, and non-strict array
   * ordering).
   *
   * @param jsonContent the expected JSON content
   */
  public ResultMatcher json(String jsonContent) {
    return json(jsonContent, false);
  }

  /**
   * Parse the response content and the given string as JSON and assert the two are "similar" -
   * i.e. they contain the same attribute-value pairs regardless of formatting.
   * <p>Can compare in two modes, depending on {@code strict} parameter value:
   * <ul>
   * <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
   * <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
   * </ul>
   * <p>Use of this matcher requires the <a
   * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
   *
   * @param jsonContent the expected JSON content
   * @param strict enables strict checking
   */
  public ResultMatcher json(String jsonContent, boolean strict) {
    return result -> {
      String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      this.jsonHelper.assertJsonEqual(jsonContent, content, strict);
    };
  }

}
