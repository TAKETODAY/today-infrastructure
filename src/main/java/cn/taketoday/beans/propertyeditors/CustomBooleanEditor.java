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

package cn.taketoday.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Property editor for Boolean/boolean properties.
 *
 * <p>This is not meant to be used as system PropertyEditor but rather as
 * locale-specific Boolean editor within custom controller code, to parse
 * UI-caused boolean strings into boolean properties of beans and check
 * them in the UI form.
 *
 * <p>In web MVC code, this editor will typically be registered with
 * {@code binder.registerCustomEditor} calls.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.validation.DataBinder#registerCustomEditor
 * @since 4.0
 */
public class CustomBooleanEditor extends PropertyEditorSupport {

  /**
   * Value of {@code "true"}.
   */
  public static final String VALUE_TRUE = "true";

  /**
   * Value of {@code "false"}.
   */
  public static final String VALUE_FALSE = "false";

  /**
   * Value of {@code "on"}.
   */
  public static final String VALUE_ON = "on";

  /**
   * Value of {@code "off"}.
   */
  public static final String VALUE_OFF = "off";

  /**
   * Value of {@code "yes"}.
   */
  public static final String VALUE_YES = "yes";

  /**
   * Value of {@code "no"}.
   */
  public static final String VALUE_NO = "no";

  /**
   * Value of {@code "1"}.
   */
  public static final String VALUE_1 = "1";

  /**
   * Value of {@code "0"}.
   */
  public static final String VALUE_0 = "0";

  @Nullable
  private final String trueString;

  @Nullable
  private final String falseString;

  private final boolean allowEmpty;

  /**
   * Create a new CustomBooleanEditor instance, with "true"/"on"/"yes"
   * and "false"/"off"/"no" as recognized String values.
   * <p>The "allowEmpty" parameter states if an empty String should
   * be allowed for parsing, i.e. get interpreted as null value.
   * Else, an IllegalArgumentException gets thrown in that case.
   *
   * @param allowEmpty if empty strings should be allowed
   */
  public CustomBooleanEditor(boolean allowEmpty) {
    this(null, null, allowEmpty);
  }

  /**
   * Create a new CustomBooleanEditor instance,
   * with configurable String values for true and false.
   * <p>The "allowEmpty" parameter states if an empty String should
   * be allowed for parsing, i.e. get interpreted as null value.
   * Else, an IllegalArgumentException gets thrown in that case.
   *
   * @param trueString the String value that represents true:
   * for example, "true" (VALUE_TRUE), "on" (VALUE_ON),
   * "yes" (VALUE_YES) or some custom value
   * @param falseString the String value that represents false:
   * for example, "false" (VALUE_FALSE), "off" (VALUE_OFF),
   * "no" (VALUE_NO) or some custom value
   * @param allowEmpty if empty strings should be allowed
   * @see #VALUE_TRUE
   * @see #VALUE_FALSE
   * @see #VALUE_ON
   * @see #VALUE_OFF
   * @see #VALUE_YES
   * @see #VALUE_NO
   */
  public CustomBooleanEditor(@Nullable String trueString, @Nullable String falseString, boolean allowEmpty) {
    this.trueString = trueString;
    this.falseString = falseString;
    this.allowEmpty = allowEmpty;
  }

  @Override
  public void setAsText(@Nullable String text) throws IllegalArgumentException {
    String input = (text != null ? text.trim() : null);
    if (this.allowEmpty && StringUtils.isEmpty(input)) {
      // Treat empty String as null value.
      setValue(null);
    }
    else if (this.trueString != null && this.trueString.equalsIgnoreCase(input)) {
      setValue(Boolean.TRUE);
    }
    else if (this.falseString != null && this.falseString.equalsIgnoreCase(input)) {
      setValue(Boolean.FALSE);
    }
    else if (this.trueString == null &&
            (VALUE_TRUE.equalsIgnoreCase(input) || VALUE_ON.equalsIgnoreCase(input) ||
                    VALUE_YES.equalsIgnoreCase(input) || VALUE_1.equals(input))) {
      setValue(Boolean.TRUE);
    }
    else if (this.falseString == null
            && (VALUE_FALSE.equalsIgnoreCase(input) || VALUE_OFF.equalsIgnoreCase(input)
            || VALUE_NO.equalsIgnoreCase(input) || VALUE_0.equals(input))) {
      setValue(Boolean.FALSE);
    }
    else {
      throw new IllegalArgumentException("Invalid boolean value [" + text + "]");
    }
  }

  @Override
  public String getAsText() {
    if (Boolean.TRUE.equals(getValue())) {
      return (this.trueString != null ? this.trueString : VALUE_TRUE);
    }
    else if (Boolean.FALSE.equals(getValue())) {
      return (this.falseString != null ? this.falseString : VALUE_FALSE);
    }
    else {
      return "";
    }
  }

}
