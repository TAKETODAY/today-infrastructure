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
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Custom {@link java.beans.PropertyEditor} for String arrays.
 *
 * <p>Strings must be in CSV format, with a customizable separator.
 * By default values in the result are trimmed of whitespace.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 * @see cn.taketoday.util.StringUtils#delimitedListToStringArray
 * @see cn.taketoday.util.StringUtils#arrayToString(Object[])
 * @since 4.0
 */
public class StringArrayPropertyEditor extends PropertyEditorSupport {

  /**
   * Default separator for splitting a String: a comma (",").
   */
  public static final String DEFAULT_SEPARATOR = ",";

  private final String separator;

  @Nullable
  private final String charsToDelete;

  private final boolean emptyArrayAsNull;

  private final boolean trimValues;

  /**
   * Create a new {@code StringArrayPropertyEditor} with the default separator
   * (a comma).
   * <p>An empty text (without elements) will be turned into an empty array.
   */
  public StringArrayPropertyEditor() {
    this(DEFAULT_SEPARATOR, null, false);
  }

  /**
   * Create a new {@code StringArrayPropertyEditor} with the given separator.
   * <p>An empty text (without elements) will be turned into an empty array.
   *
   * @param separator the separator to use for splitting a {@link String}
   */
  public StringArrayPropertyEditor(String separator) {
    this(separator, null, false);
  }

  /**
   * Create a new {@code StringArrayPropertyEditor} with the given separator.
   *
   * @param separator the separator to use for splitting a {@link String}
   * @param emptyArrayAsNull {@code true} if an empty String array
   * is to be transformed into {@code null}
   */
  public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull) {
    this(separator, null, emptyArrayAsNull);
  }

  /**
   * Create a new {@code StringArrayPropertyEditor} with the given separator.
   *
   * @param separator the separator to use for splitting a {@link String}
   * @param emptyArrayAsNull {@code true} if an empty String array
   * is to be transformed into {@code null}
   * @param trimValues {@code true} if the values in the parsed arrays
   * are to be trimmed of whitespace (default is true)
   */
  public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull, boolean trimValues) {
    this(separator, null, emptyArrayAsNull, trimValues);
  }

  /**
   * Create a new {@code StringArrayPropertyEditor} with the given separator.
   *
   * @param separator the separator to use for splitting a {@link String}
   * @param charsToDelete a set of characters to delete, in addition to
   * trimming an input String. Useful for deleting unwanted line breaks:
   * e.g. "\r\n\f" will delete all new lines and line feeds in a String.
   * @param emptyArrayAsNull {@code true} if an empty String array
   * is to be transformed into {@code null}
   */
  public StringArrayPropertyEditor(String separator, @Nullable String charsToDelete, boolean emptyArrayAsNull) {
    this(separator, charsToDelete, emptyArrayAsNull, true);
  }

  /**
   * Create a new {@code StringArrayPropertyEditor} with the given separator.
   *
   * @param separator the separator to use for splitting a {@link String}
   * @param charsToDelete a set of characters to delete, in addition to
   * trimming an input String. Useful for deleting unwanted line breaks:
   * e.g. "\r\n\f" will delete all new lines and line feeds in a String.
   * @param emptyArrayAsNull {@code true} if an empty String array
   * is to be transformed into {@code null}
   * @param trimValues {@code true} if the values in the parsed arrays
   * are to be trimmed of whitespace (default is true)
   */
  public StringArrayPropertyEditor(
          String separator, @Nullable String charsToDelete, boolean emptyArrayAsNull, boolean trimValues) {

    this.separator = separator;
    this.charsToDelete = charsToDelete;
    this.emptyArrayAsNull = emptyArrayAsNull;
    this.trimValues = trimValues;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    String[] array = StringUtils.delimitedListToStringArray(text, this.separator, this.charsToDelete);
    if (this.emptyArrayAsNull && array.length == 0) {
      setValue(null);
    }
    else {
      if (this.trimValues) {
        array = StringUtils.trimArrayElements(array);
      }
      setValue(array);
    }
  }

  @Override
  public String getAsText() {
    return StringUtils.arrayToString(ObjectUtils.toObjectArray(getValue()), this.separator);
  }

}
