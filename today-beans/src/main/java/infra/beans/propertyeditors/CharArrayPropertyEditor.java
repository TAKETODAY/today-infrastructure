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

import infra.lang.Nullable;

/**
 * Editor for char arrays. Strings will simply be converted to
 * their corresponding char representations.
 *
 * @author Juergen Hoeller
 * @see String#toCharArray()
 * @since 4.0
 */
public class CharArrayPropertyEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(@Nullable String text) {
    setValue(text != null ? text.toCharArray() : null);
  }

  @Override
  public String getAsText() {
    char[] value = (char[]) getValue();
    return (value != null ? new String(value) : "");
  }

}
