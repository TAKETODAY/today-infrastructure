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
import java.util.TimeZone;

import cn.taketoday.util.StringUtils;

/**
 * Editor for {@code java.util.TimeZone}, translating timezone IDs into
 * {@code TimeZone} objects. Exposes the {@code TimeZone} ID as a text
 * representation.
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @see TimeZone
 * @see ZoneIdEditor
 * @since 4.0
 */
public class TimeZoneEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      text = text.trim();
    }
    setValue(StringUtils.parseTimeZoneString(text));
  }

  @Override
  public String getAsText() {
    TimeZone value = (TimeZone) getValue();
    return (value != null ? value.getID() : "");
  }

}
