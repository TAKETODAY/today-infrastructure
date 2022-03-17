/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.style;

import cn.taketoday.lang.Nullable;

/**
 * A strategy interface for pretty-printing {@code toString()} methods.
 * Encapsulates the print algorithms; some other object such as a builder
 * should provide the workflow.
 *
 * @author Keith Donald
 * @since 4.0
 */
public interface ToStringStyler {

  /**
   * Style a {@code toString()}'ed object before its fields are styled.
   *
   * @param buffer the buffer to print to
   * @param obj the object to style
   */
  void styleStart(StringBuilder buffer, Object obj);

  /**
   * Style a {@code toString()}'ed object after it's fields are styled.
   *
   * @param buffer the buffer to print to
   * @param obj the object to style
   */
  void styleEnd(StringBuilder buffer, Object obj);

  /**
   * Style a field value as a string.
   *
   * @param buffer the buffer to print to
   * @param fieldName the he name of the field
   * @param value the field value
   */
  void styleField(StringBuilder buffer, String fieldName, @Nullable Object value);

  /**
   * Style the given value.
   *
   * @param buffer the buffer to print to
   * @param value the field value
   */
  void styleValue(StringBuilder buffer, Object value);

  /**
   * Style the field separator.
   *
   * @param buffer the buffer to print to
   */
  void styleFieldSeparator(StringBuilder buffer);

}
