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

/**
 * Editor for char arrays. Strings will simply be converted to
 * their corresponding char representations.
 *
 * @author Juergen Hoeller
 * @see String#toCharArray()
 * @since 1.2.8
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
