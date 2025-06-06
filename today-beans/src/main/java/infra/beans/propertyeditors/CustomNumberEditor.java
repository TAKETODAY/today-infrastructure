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

package infra.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.text.NumberFormat;

import infra.lang.Nullable;
import infra.util.NumberUtils;
import infra.util.StringUtils;

/**
 * Property editor for any Number subclass such as Short, Integer, Long,
 * BigInteger, Float, Double, BigDecimal. Can use a given NumberFormat for
 * (locale-specific) parsing and rendering, or alternatively the default
 * {@code decode} / {@code valueOf} / {@code toString} methods.
 *
 * <p>This is not meant to be used as system PropertyEditor but rather
 * as locale-specific number editor within custom controller code,
 * parsing user-entered number strings into Number properties of beans
 * and rendering them in the UI form.
 *
 * <p>In web MVC code, this editor will typically be registered with
 * {@code binder.registerCustomEditor} calls.
 *
 * @author Juergen Hoeller
 * @see Number
 * @see NumberFormat
 * @see infra.validation.DataBinder#registerCustomEditor
 * @since 4.0
 */
public class CustomNumberEditor extends PropertyEditorSupport {

  private final Class<? extends Number> numberClass;

  @Nullable
  private final NumberFormat numberFormat;

  private final boolean allowEmpty;

  /**
   * Create a new CustomNumberEditor instance, using the default
   * {@code valueOf} methods for parsing and {@code toString}
   * methods for rendering.
   * <p>The "allowEmpty" parameter states if an empty String should
   * be allowed for parsing, i.e. get interpreted as {@code null} value.
   * Else, an IllegalArgumentException gets thrown in that case.
   *
   * @param numberClass the Number subclass to generate
   * @param allowEmpty if empty strings should be allowed
   * @throws IllegalArgumentException if an invalid numberClass has been specified
   * @see NumberUtils#parseNumber(String, Class)
   * @see Integer#valueOf
   * @see Integer#toString
   */
  public CustomNumberEditor(Class<? extends Number> numberClass, boolean allowEmpty) throws IllegalArgumentException {
    this(numberClass, null, allowEmpty);
  }

  /**
   * Create a new CustomNumberEditor instance, using the given NumberFormat
   * for parsing and rendering.
   * <p>The allowEmpty parameter states if an empty String should
   * be allowed for parsing, i.e. get interpreted as {@code null} value.
   * Else, an IllegalArgumentException gets thrown in that case.
   *
   * @param numberClass the Number subclass to generate
   * @param numberFormat the NumberFormat to use for parsing and rendering
   * @param allowEmpty if empty strings should be allowed
   * @throws IllegalArgumentException if an invalid numberClass has been specified
   * @see NumberUtils#parseNumber(String, Class, NumberFormat)
   * @see NumberFormat#parse
   * @see NumberFormat#format
   */
  public CustomNumberEditor(Class<? extends Number> numberClass,
                            @Nullable NumberFormat numberFormat, boolean allowEmpty) throws IllegalArgumentException {

    if (!Number.class.isAssignableFrom(numberClass)) {
      throw new IllegalArgumentException("Property class must be a subclass of Number");
    }
    this.numberClass = numberClass;
    this.numberFormat = numberFormat;
    this.allowEmpty = allowEmpty;
  }

  /**
   * Parse the Number from the given text, using the specified NumberFormat.
   */
  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (this.allowEmpty && StringUtils.isBlank(text)) {
      // Treat empty String as null value.
      setValue(null);
    }
    else if (this.numberFormat != null) {
      // Use given NumberFormat for parsing text.
      setValue(NumberUtils.parseNumber(text, this.numberClass, this.numberFormat));
    }
    else {
      // Use default valueOf methods for parsing text.
      setValue(NumberUtils.parseNumber(text, this.numberClass));
    }
  }

  /**
   * Coerce a Number value into the required target class, if necessary.
   */
  @Override
  public void setValue(@Nullable Object value) {
    if (value instanceof Number) {
      super.setValue(NumberUtils.convertNumberToTargetClass((Number) value, this.numberClass));
    }
    else {
      super.setValue(value);
    }
  }

  /**
   * Format the Number as String, using the specified NumberFormat.
   */
  @Override
  public String getAsText() {
    Object value = getValue();
    if (value == null) {
      return "";
    }
    if (this.numberFormat != null) {
      // Use NumberFormat for rendering value.
      return this.numberFormat.format(value);
    }
    else {
      // Use toString method for rendering value.
      return value.toString();
    }
  }

}
