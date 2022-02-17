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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Property editor for {@code java.util.Date},
 * supporting a custom {@code java.text.DateFormat}.
 *
 * <p>This is not meant to be used as system PropertyEditor but rather
 * as locale-specific date editor within custom controller code,
 * parsing user-entered number strings into Date properties of beans
 * and rendering them in the UI form.
 *
 * <p>In web MVC code, this editor will typically be registered with
 * {@code binder.registerCustomEditor}.
 *
 * @author Juergen Hoeller
 * @see Date
 * @see DateFormat
 * @see cn.taketoday.validation.DataBinder#registerCustomEditor
 * @since 4.0
 */
public class CustomDateEditor extends PropertyEditorSupport {

  private final DateFormat dateFormat;

  private final boolean allowEmpty;

  private final int exactDateLength;

  /**
   * Create a new CustomDateEditor instance, using the given DateFormat
   * for parsing and rendering.
   * <p>The "allowEmpty" parameter states if an empty String should
   * be allowed for parsing, i.e. get interpreted as null value.
   * Otherwise, an IllegalArgumentException gets thrown in that case.
   *
   * @param dateFormat the DateFormat to use for parsing and rendering
   * @param allowEmpty if empty strings should be allowed
   */
  public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty) {
    this.dateFormat = dateFormat;
    this.allowEmpty = allowEmpty;
    this.exactDateLength = -1;
  }

  /**
   * Create a new CustomDateEditor instance, using the given DateFormat
   * for parsing and rendering.
   * <p>The "allowEmpty" parameter states if an empty String should
   * be allowed for parsing, i.e. get interpreted as null value.
   * Otherwise, an IllegalArgumentException gets thrown in that case.
   * <p>The "exactDateLength" parameter states that IllegalArgumentException gets
   * thrown if the String does not exactly match the length specified. This is useful
   * because SimpleDateFormat does not enforce strict parsing of the year part,
   * not even with {@code setLenient(false)}. Without an "exactDateLength"
   * specified, the "01/01/05" would get parsed to "01/01/0005". However, even
   * with an "exactDateLength" specified, prepended zeros in the day or month
   * part may still allow for a shorter year part, so consider this as just
   * one more assertion that gets you closer to the intended date format.
   *
   * @param dateFormat the DateFormat to use for parsing and rendering
   * @param allowEmpty if empty strings should be allowed
   * @param exactDateLength the exact expected length of the date String
   */
  public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty, int exactDateLength) {
    this.dateFormat = dateFormat;
    this.allowEmpty = allowEmpty;
    this.exactDateLength = exactDateLength;
  }

  /**
   * Parse the Date from the given text, using the specified DateFormat.
   */
  @Override
  public void setAsText(@Nullable String text) throws IllegalArgumentException {
    if (this.allowEmpty && !StringUtils.hasText(text)) {
      // Treat empty String as null value.
      setValue(null);
    }
    else if (text != null && this.exactDateLength >= 0 && text.length() != this.exactDateLength) {
      throw new IllegalArgumentException(
              "Could not parse date: it is not exactly" + this.exactDateLength + "characters long");
    }
    else {
      try {
        setValue(this.dateFormat.parse(text));
      }
      catch (ParseException ex) {
        throw new IllegalArgumentException("Could not parse date: " + ex.getMessage(), ex);
      }
    }
  }

  /**
   * Format the Date as String, using the specified DateFormat.
   */
  @Override
  public String getAsText() {
    Date value = (Date) getValue();
    return (value != null ? this.dateFormat.format(value) : "");
  }

}
