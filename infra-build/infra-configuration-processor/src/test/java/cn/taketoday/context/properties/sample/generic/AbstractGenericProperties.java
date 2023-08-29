/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.sample.generic;

import java.util.HashMap;
import java.util.Map;

/**
 * A base properties class with generics.
 *
 * @param <A> name type
 * @param <B> mapping key type
 * @param <C> mapping value type
 * @author Stephane Nicoll
 */
public class AbstractGenericProperties<A, B, C> {

  /**
   * Generic name.
   */
  private A name;

  /**
   * Generic mappings.
   */
  private final Map<B, C> mappings = new HashMap<>();

  public A getName() {
    return this.name;
  }

  public void setName(A name) {
    this.name = name;
  }

  public Map<B, C> getMappings() {
    return this.mappings;
  }

}
