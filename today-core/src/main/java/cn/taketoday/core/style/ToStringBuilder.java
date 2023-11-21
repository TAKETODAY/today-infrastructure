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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Utility class that builds pretty-printing {@code toString()} methods
 * with pluggable styling conventions.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ToStringBuilder {
  /**
   * Default ValueStyler instance used by the {@code style} method.
   * Also available for the {@link ToStringBuilder} class in this package.
   */
  static final DefaultValueStyler DEFAULT_VALUE_STYLER = new DefaultValueStyler();

  /**
   * Default ToStringStyler instance used by this ToStringBuilder.
   */
  private static final ToStringStyler DEFAULT_TO_STRING_STYLER =
          new DefaultToStringStyler(DEFAULT_VALUE_STYLER);

  private final StringBuilder buffer = new StringBuilder(256);

  private final ToStringStyler styler;

  private final Object object;

  private boolean styledFirstField;

  /**
   * Create a ToStringBuilder for the given object.
   *
   * @param obj the object to be stringified
   */
  public ToStringBuilder(Object obj) {
    this(obj, (ToStringStyler) null);
  }

  /**
   * Create a ToStringBuilder for the given object, using the provided style.
   *
   * @param obj the object to be stringified
   * @param styler the ValueStyler encapsulating pretty-print instructions
   */
  public ToStringBuilder(Object obj, @Nullable ValueStyler styler) {
    this(obj, new DefaultToStringStyler(styler != null ? styler : DEFAULT_VALUE_STYLER));
  }

  /**
   * Create a ToStringBuilder for the given object, using the provided style.
   *
   * @param obj the object to be stringified
   * @param styler the ToStringStyler encapsulating pretty-print instructions
   */
  public ToStringBuilder(Object obj, @Nullable ToStringStyler styler) {
    Assert.notNull(obj, "The object to be styled is required");
    this.object = obj;
    this.styler = (styler != null ? styler : DEFAULT_TO_STRING_STYLER);
    this.styler.styleStart(this.buffer, this.object);
  }

  /**
   * Append a byte field value.
   *
   * @param fieldName the name of the field, usually the member variable name
   * @param value the field value
   * @return this, to support call-chaining
   */
  public ToStringBuilder append(String fieldName, byte value) {
    return append(fieldName, Byte.valueOf(value));
  }

  /**
   * Append a short field value.
   *
   * @param fieldName the name of the field, usually the member variable name
   * @param value the field value
   * @return this, to support call-chaining
   */
  public ToStringBuilder append(String fieldName, short value) {
    return append(fieldName, Short.valueOf(value));
  }

  /**
   * Append a integer field value.
   *
   * @param fieldName the name of the field, usually the member variable name
   * @param value the field value
   * @return this, to support call-chaining
   */
  public ToStringBuilder append(String fieldName, int value) {
    return append(fieldName, Integer.valueOf(value));
  }

  /**
   * Append a long field value.
   *
   * @param fieldName the name of the field, usually the member variable name
   * @param value the field value
   * @return this, to support call-chaining
   */
  public ToStringBuilder append(String fieldName, long value) {
    return append(fieldName, Long.valueOf(value));
  }

  /**
   * Append a float field value.
   *
   * @param fieldName the name of the field, usually the member variable name
   * @param value the field value
   * @return this, to support call-chaining
   */
  public ToStringBuilder append(String fieldName, float value) {
    return append(fieldName, Float.valueOf(value));
  }

  /**
   * Append a double field value.
   *
   * @param fieldName the name of the field, usually the member variable name
   * @param value the field value
   * @return this, to support call-chaining
   */
  public ToStringBuilder append(String fieldName, double value) {
    return append(fieldName, Double.valueOf(value));
  }

  /**
   * Append a boolean field value.
   *
   * @param fieldName the name of the field, usually the member variable name
   * @param value the field value
   * @return this, to support call-chaining
   */
  public ToStringBuilder append(String fieldName, boolean value) {
    return append(fieldName, Boolean.valueOf(value));
  }

  /**
   * Append a field value.
   *
   * @param fieldName the name of the field, usually the member variable name
   * @param value the field value
   * @return this, to support call-chaining
   */
  public ToStringBuilder append(String fieldName, @Nullable Object value) {
    printFieldSeparatorIfNecessary();
    this.styler.styleField(this.buffer, fieldName, value);
    return this;
  }

  private void printFieldSeparatorIfNecessary() {
    if (this.styledFirstField) {
      this.styler.styleFieldSeparator(this.buffer);
    }
    else {
      this.styledFirstField = true;
    }
  }

  /**
   * Append the provided value.
   *
   * @param value the value to append
   * @return this, to support call-chaining.
   */
  public ToStringBuilder append(Object value) {
    this.styler.styleValue(this.buffer, value);
    return this;
  }

  /**
   * Return the String representation that this ToStringBuilder built.
   */
  @Override
  public String toString() {
    this.styler.styleEnd(this.buffer, this.object);
    return this.buffer.toString();
  }

  public static ToStringBuilder from(Object obj) {
    return new ToStringBuilder(obj);
  }

}
