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

package cn.taketoday.test.util;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Test assertions that are independent of any third-party assertion library.
 *
 * @author Lukas Krecan
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class AssertionErrors {

  /**
   * Fail a test with the given message.
   *
   * @param message a message that describes the reason for the failure
   */
  public static void fail(String message) {
    throw new AssertionError(message);
  }

  /**
   * Fail a test with the given message passing along expected and actual
   * values to be appended to the message.
   * <p>For example given:
   * <pre class="code">
   * String name = "Accept";
   * String expected = "application/json";
   * String actual = "text/plain";
   * fail("Response header [" + name + "]", expected, actual);
   * </pre>
   * <p>The resulting message is:
   * <pre class="code">
   * Response header [Accept] expected:&lt;application/json&gt; but was:&lt;text/plain&gt;
   * </pre>
   *
   * @param message a message that describes the use case that failed
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void fail(String message, @Nullable Object expected, @Nullable Object actual) {
    throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
  }

  /**
   * Assert the given condition is {@code true} and raise an
   * {@link AssertionError} otherwise.
   *
   * @param message a message that describes the reason for the failure
   * @param condition the condition to test for
   */
  public static void assertTrue(String message, boolean condition) {
    if (!condition) {
      fail(message);
    }
  }

  /**
   * Assert the given condition is {@code false} and raise an
   * {@link AssertionError} otherwise.
   *
   * @param message a message that describes the reason for the failure
   * @param condition the condition to test for
   */
  public static void assertFalse(String message, boolean condition) {
    if (condition) {
      fail(message);
    }
  }

  /**
   * Assert that the given object is {@code null} and raise an
   * {@link AssertionError} otherwise.
   *
   * @param message a message that describes the reason for the failure
   * @param object the object to check
   */
  public static void assertNull(String message, @Nullable Object object) {
    assertTrue(message, object == null);
  }

  /**
   * Assert that the given object is not {@code null} and raise an
   * {@link AssertionError} otherwise.
   *
   * @param message a message that describes the reason for the failure
   * @param object the object to check
   */
  public static void assertNotNull(String message, @Nullable Object object) {
    assertTrue(message, object != null);
  }

  /**
   * Assert two objects are equal and raise an {@link AssertionError} otherwise.
   * <p>For example:
   * <pre class="code">
   * assertEquals("Response header [" + name + "]", expected, actual);
   * </pre>
   *
   * @param message a message that describes the value being checked
   * @param expected the expected value
   * @param actual the actual value
   * @see #fail(String, Object, Object)
   */
  public static void assertEquals(String message, @Nullable Object expected, @Nullable Object actual) {
    if (!ObjectUtils.nullSafeEquals(expected, actual)) {
      fail(message, ObjectUtils.nullSafeToString(expected), ObjectUtils.nullSafeToString(actual));
    }
  }

  /**
   * Assert two objects are not equal and raise an {@link AssertionError} otherwise.
   * <p>For example:
   * <pre class="code">
   * assertNotEquals("Response header [" + name + "]", expected, actual);
   * </pre>
   *
   * @param message a message that describes the value being checked
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertNotEquals(String message, @Nullable Object expected, @Nullable Object actual) {
    if (ObjectUtils.nullSafeEquals(expected, actual)) {
      throw new AssertionError(message + " was not expected to be:" +
              "<" + ObjectUtils.nullSafeToString(actual) + ">");
    }
  }

}
