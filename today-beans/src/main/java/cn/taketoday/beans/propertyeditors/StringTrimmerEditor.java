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
 * Property editor that trims Strings.
 *
 * <p>Optionally allows transforming an empty string into a {@code null} value.
 * Needs to be explicitly registered, e.g. for command binding.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.validation.DataBinder#registerCustomEditor
 * @since 4.0
 */
public class StringTrimmerEditor extends PropertyEditorSupport {

  @Nullable
  private final String charsToDelete;

  private final boolean emptyAsNull;

  /**
   * Create a new StringTrimmerEditor.
   *
   * @param emptyAsNull {@code true} if an empty String is to be
   * transformed into {@code null}
   */
  public StringTrimmerEditor(boolean emptyAsNull) {
    this.charsToDelete = null;
    this.emptyAsNull = emptyAsNull;
  }

  /**
   * Create a new StringTrimmerEditor.
   *
   * @param charsToDelete a set of characters to delete, in addition to
   * trimming an input String. Useful for deleting unwanted line breaks:
   * e.g. "\r\n\f" will delete all new lines and line feeds in a String.
   * @param emptyAsNull {@code true} if an empty String is to be
   * transformed into {@code null}
   */
  public StringTrimmerEditor(String charsToDelete, boolean emptyAsNull) {
    this.charsToDelete = charsToDelete;
    this.emptyAsNull = emptyAsNull;
  }

  @Override
  public void setAsText(@Nullable String text) {
    if (text == null) {
      setValue(null);
    }
    else {
      String value = text.trim();
      if (this.charsToDelete != null) {
        value = StringUtils.deleteAny(value, this.charsToDelete);
      }
      if (this.emptyAsNull && value.isEmpty()) {
        setValue(null);
      }
      else {
        setValue(value);
      }
    }
  }

  @Override
  public String getAsText() {
    Object value = getValue();
    return (value != null ? value.toString() : "");
  }

}
