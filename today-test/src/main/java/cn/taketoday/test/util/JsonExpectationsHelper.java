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

package cn.taketoday.test.util;

import org.skyscreamer.jsonassert.JSONAssert;

/**
 * A helper class for assertions on JSON content.
 *
 * <p>Use of this class requires the <a
 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class JsonExpectationsHelper {

  /**
   * Parse the expected and actual strings as JSON and assert the two
   * are "similar" - i.e. they contain the same attribute-value pairs
   * regardless of formatting with lenient checking (extensible content and
   * non-strict array ordering).
   *
   * @param expected the expected JSON content
   * @param actual the actual JSON content
   * @see #assertJsonEqual(String, String, boolean)
   */
  public void assertJsonEqual(String expected, String actual) throws Exception {
    assertJsonEqual(expected, actual, false);
  }

  /**
   * Parse the expected and actual strings as JSON and assert the two
   * are "similar" - i.e. they contain the same attribute-value pairs
   * regardless of formatting.
   * <p>Can compare in two modes, depending on the {@code strict} parameter value:
   * <ul>
   * <li>{@code true}: strict checking. Not extensible and strict array ordering.</li>
   * <li>{@code false}: lenient checking. Extensible and non-strict array ordering.</li>
   * </ul>
   *
   * @param expected the expected JSON content
   * @param actual the actual JSON content
   * @param strict enables strict checking if {@code true}
   */
  public void assertJsonEqual(String expected, String actual, boolean strict) throws Exception {
    JSONAssert.assertEquals(expected, actual, strict);
  }

  /**
   * Parse the expected and actual strings as JSON and assert the two
   * are "not similar" - i.e. they contain different attribute-value pairs
   * regardless of formatting with a lenient checking (extensible, and non-strict
   * array ordering).
   *
   * @param expected the expected JSON content
   * @param actual the actual JSON content
   * @see #assertJsonNotEqual(String, String, boolean)
   */
  public void assertJsonNotEqual(String expected, String actual) throws Exception {
    assertJsonNotEqual(expected, actual, false);
  }

  /**
   * Parse the expected and actual strings as JSON and assert the two
   * are "not similar" - i.e. they contain different attribute-value pairs
   * regardless of formatting.
   * <p>Can compare in two modes, depending on {@code strict} parameter value:
   * <ul>
   * <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
   * <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
   * </ul>
   *
   * @param expected the expected JSON content
   * @param actual the actual JSON content
   * @param strict enables strict checking
   */
  public void assertJsonNotEqual(String expected, String actual, boolean strict) throws Exception {
    JSONAssert.assertNotEquals(expected, actual, strict);
  }

}
