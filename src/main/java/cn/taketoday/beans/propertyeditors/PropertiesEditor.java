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
import java.util.Map;
import java.util.Properties;

import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.lang.Nullable;

/**
 * Custom {@link java.beans.PropertyEditor} for {@link Properties} objects.
 *
 * <p>Handles conversion from content {@link String} to {@code Properties} object.
 * Also handles {@link Map} to {@code Properties} conversion, for populating
 * a {@code Properties} object via XML "map" entries.
 *
 * <p>The required format is defined in the standard {@code Properties}
 * documentation. Each property must be on a new line.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see Properties#load
 * @since 4.0
 */
public class PropertiesEditor extends PropertyEditorSupport {

  /**
   * Convert {@link String} into {@link Properties}, considering it as
   * properties content.
   *
   * @param text the text to be so converted
   */
  @Override
  public void setAsText(@Nullable String text) throws IllegalArgumentException {
    setValue(PropertiesUtils.parse(text));
  }

  /**
   * Take {@link Properties} as-is; convert {@link Map} into {@code Properties}.
   */
  @Override
  public void setValue(Object value) {
    if (!(value instanceof Properties) && value instanceof Map) {
      Properties props = new Properties();
      props.putAll((Map<?, ?>) value);
      super.setValue(props);
    }
    else {
      super.setValue(value);
    }
  }

}
