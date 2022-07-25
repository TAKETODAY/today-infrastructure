/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.result;

/**
 * Represents a result set column
 */
public final class Column {

  private final String name;
  private final int index;
  private final String type;

  public Column(String name, int index, String type) {
    this.name = name;
    this.index = index;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public int getIndex() {
    return index;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return getName() + " (" + getType() + ")";
  }
}
