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
package cn.taketoday.web;

import java.io.Serial;
import java.util.function.Supplier;

import cn.taketoday.http.HttpStatus;

/**
 * 404 NotFound
 *
 * @author TODAY
 * @since 2018-11-26 20:04
 */
public class NotFoundException extends ResponseStatusException {
  @Serial
  private static final long serialVersionUID = 1L;
  public static final String NOT_FOUND = HttpStatus.NOT_FOUND.getReasonPhrase();

  public NotFoundException(Throwable cause) {
    super(HttpStatus.NOT_FOUND, null, cause);
  }

  public NotFoundException(String message, Throwable cause) {
    super(HttpStatus.NOT_FOUND, message, cause);
  }

  public NotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
  }

  public NotFoundException() {
    super(HttpStatus.NOT_FOUND, NOT_FOUND);
  }

  /**
   * Assert that an object is not {@code null}.
   */
  public static void notNull(Object object) {
    notNull(object, NOT_FOUND);
  }

  /**
   * Assert that an object is not {@code null}.
   * <pre class="code">NotFoundException.notNull(clazz, "The class must not be null");</pre>
   *
   * @param object the object to check
   * @param message the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the object is {@code null}
   */
  public static void notNull(Object object, String message) {
    if (object == null) {
      throw new NotFoundException(message);
    }
  }

  /**
   * Assert that an object is not {@code null}. <pre class="code">
   * NotFoundException.notNull(clazz, () -&gt; "The class '" + clazz.getName() + "' must not be null");
   * </pre>
   *
   * @param object the object to check
   * @param messageSupplier a supplier for the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the object is {@code null}
   */
  public static void notNull(Object object, Supplier<String> messageSupplier) {
    if (object == null) {
      throw new NotFoundException(nullSafeGet(messageSupplier));
    }
  }

  /**
   * Assert a boolean expression, throwing an {@code IllegalArgumentException} if
   * the expression evaluates to {@code false}. <pre class="code">
   * Assert.isTrue(i &gt; 0, () -&gt; "The value '" + i + "' must be greater than zero");
   * </pre>
   *
   * @param expression a boolean expression
   * @param messageSupplier a supplier for the exception message to use if the assertion fails
   * @throws IllegalArgumentException if {@code expression} is {@code false}
   */
  public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
    if (!expression) {
      throw new NotFoundException(nullSafeGet(messageSupplier));
    }
  }

  private static String nullSafeGet(Supplier<String> messageSupplier) {
    return messageSupplier != null ? messageSupplier.get() : null;
  }

}
