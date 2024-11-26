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
import java.time.DateTimeException;
import java.time.ZoneId;

import infra.util.StringUtils;

/**
 * Editor for {@code java.time.ZoneId}, translating zone ID Strings into {@code ZoneId}
 * objects. Exposes the {@code TimeZone} ID as a text representation.
 *
 * @author Nicholas Williams
 * @see ZoneId
 * @see TimeZoneEditor
 * @since 4.0
 */
public class ZoneIdEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      text = text.trim();
    }
    try {
      setValue(ZoneId.of(text));
    }
    catch (DateTimeException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  @Override
  public String getAsText() {
    ZoneId value = (ZoneId) getValue();
    return (value != null ? value.getId() : "");
  }

}
