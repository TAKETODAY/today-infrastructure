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

package example;

import java.util.HashMap;
import java.util.Map;

/**
 * Type that can be indexed by the {@link Color} enum (i.e., something other
 * than an int, Integer, or String) and whose indexed values are Strings.
 */
public class FruitMap {

  private final Map<Color, String> map = new HashMap<>();

  public FruitMap() {
    this.map.put(Color.RED, "cherry");
    this.map.put(Color.ORANGE, "orange");
    this.map.put(Color.YELLOW, "banana");
    this.map.put(Color.GREEN, "kiwi");
    this.map.put(Color.BLUE, "blueberry");
    // We don't map PURPLE so that we can test for an unsupported color.
  }

  public String getFruit(Color color) {
    if (!this.map.containsKey(color)) {
      throw new IllegalArgumentException("No fruit for color " + color);
    }
    return this.map.get(color);
  }

  public void setFruit(Color color, String fruit) {
    this.map.put(color, fruit);
  }

}
