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

package cn.taketoday.session;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when an HTTP request handler requires a pre-existing session.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:42
 */
public class WebSessionRequiredException extends NestedRuntimeException {

  @Nullable
  private final String expectedAttribute;

  /**
   * Create a new WebSessionRequiredException.
   *
   * @param msg the detail message
   */
  public WebSessionRequiredException(String msg) {
    super(msg);
    this.expectedAttribute = null;
  }

  /**
   * Create a new WebSessionRequiredException.
   *
   * @param msg the detail message
   * @param expectedAttribute the name of the expected session attribute
   */
  public WebSessionRequiredException(String msg, String expectedAttribute) {
    super(msg);
    this.expectedAttribute = expectedAttribute;
  }

  /**
   * Return the name of the expected session attribute, if any.
   */
  @Nullable
  public String getExpectedAttribute() {
    return this.expectedAttribute;
  }

}
