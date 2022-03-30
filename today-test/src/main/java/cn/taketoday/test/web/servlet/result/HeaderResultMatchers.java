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

package cn.taketoday.test.web.servlet.result;

import org.hamcrest.Matcher;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.test.web.servlet.ResultMatcher;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.assertFalse;
import static cn.taketoday.test.util.AssertionErrors.assertNotNull;
import static cn.taketoday.test.util.AssertionErrors.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for response header assertions.
 *
 * <p>An instance of this class is available via
 * {@link MockMvcResultMatchers#header}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.0
 */
public class HeaderResultMatchers {

  /**
   * Protected constructor.
   * See {@link MockMvcResultMatchers#header()}.
   */
  protected HeaderResultMatchers() {
  }

  /**
   * Assert the primary value of the response header with the given Hamcrest
   * String {@code Matcher}.
   */
  public ResultMatcher string(String name, Matcher<? super String> matcher) {
    return result -> assertThat("Response header '" + name + "'", result.getResponse().getHeader(name), matcher);
  }

  /**
   * Assert the values of the response header with the given Hamcrest
   * Iterable {@link Matcher}.
   *
   * @since 4.3
   */
  public ResultMatcher stringValues(String name, Matcher<? super Iterable<String>> matcher) {
    return result -> {
      List<String> values = result.getResponse().getHeaders(name);
      assertThat("Response header '" + name + "'", values, matcher);
    };
  }

  /**
   * Assert the primary value of the response header as a String value.
   */
  public ResultMatcher string(String name, String value) {
    return result -> assertEquals("Response header '" + name + "'", value, result.getResponse().getHeader(name));
  }

  /**
   * Assert the values of the response header as String values.
   *
   * @since 4.3
   */
  public ResultMatcher stringValues(String name, String... values) {
    return result -> {
      List<Object> actual = result.getResponse().getHeaderValues(name);
      assertEquals("Response header '" + name + "'", Arrays.asList(values), actual);
    };
  }

  /**
   * Assert that the named response header exists.
   *
   * @since 5.0.3
   */
  public ResultMatcher exists(String name) {
    return result -> assertTrue("Response should contain header '" + name + "'",
            result.getResponse().containsHeader(name));
  }

  /**
   * Assert that the named response header does not exist.
   *
   * @since 4.0
   */
  public ResultMatcher doesNotExist(String name) {
    return result -> assertFalse("Response should not contain header '" + name + "'",
            result.getResponse().containsHeader(name));
  }

  /**
   * Assert the primary value of the named response header as a {@code long}.
   * <p>The {@link ResultMatcher} returned by this method throws an
   * {@link AssertionError} if the response does not contain the specified
   * header, or if the supplied {@code value} does not match the primary value.
   */
  public ResultMatcher longValue(String name, long value) {
    return result -> {
      MockHttpServletResponse response = result.getResponse();
      assertTrue("Response does not contain header '" + name + "'", response.containsHeader(name));
      String headerValue = response.getHeader(name);
      if (headerValue != null) {
        assertEquals("Response header '" + name + "'", value, Long.parseLong(headerValue));
      }
    };
  }

  /**
   * Assert the primary value of the named response header parsed into a date
   * using the preferred date format described in RFC 7231.
   * <p>The {@link ResultMatcher} returned by this method throws an
   * {@link AssertionError} if the response does not contain the specified
   * header, or if the supplied {@code value} does not match the primary value.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
   * @since 4.2
   */
  public ResultMatcher dateValue(String name, long value) {
    return result -> {
      MockHttpServletResponse response = result.getResponse();
      String headerValue = response.getHeader(name);
      assertNotNull("Response does not contain header '" + name + "'", headerValue);

      HttpHeaders headers = HttpHeaders.create();
      headers.setDate("expected", value);
      headers.set("actual", headerValue);

      assertEquals("Response header '" + name + "'='" + headerValue + "' " +
                      "does not match expected value '" + headers.getFirst("expected") + "'",
              headers.getFirstDate("expected"), headers.getFirstDate("actual"));
    };
  }

}
