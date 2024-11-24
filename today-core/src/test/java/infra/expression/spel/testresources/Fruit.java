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

package infra.expression.spel.testresources;

import java.awt.Color;

///CLOVER:OFF
public class Fruit {
  public String name; // accessible as property field
  public Color color; // accessible as property through getter/setter
  public String colorName; // accessible as property through getter/setter
  public int stringscount = -1;

  public Fruit(String name, Color color, String colorName) {
    this.name = name;
    this.color = color;
    this.colorName = colorName;
  }

  public Color getColor() {
    return color;
  }

  public Fruit(String... strings) {
    stringscount = strings.length;
  }

  public Fruit(int i, String... strings) {
    stringscount = i + strings.length;
  }

  public int stringscount() {
    return stringscount;
  }

  @Override
  public String toString() {
    return "A" + (colorName != null && colorName.startsWith("o") ? "n " : " ") + colorName + " " + name;
  }
}
