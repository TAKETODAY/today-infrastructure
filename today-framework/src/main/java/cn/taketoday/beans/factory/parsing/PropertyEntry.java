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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.util.StringUtils;

/**
 * {@link ParseState} entry representing a JavaBean property.
 *
 * @author Rob Harrop
 * @since 4.0
 */
public class PropertyEntry implements ParseState.Entry {

  private final String name;

  /**
   * Create a new {@code PropertyEntry} instance.
   *
   * @param name the name of the JavaBean property represented by this instance
   */
  public PropertyEntry(String name) {
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("Invalid property name '" + name + "'");
    }
    this.name = name;
  }

  @Override
  public String toString() {
    return "Property '" + this.name + "'";
  }

}
