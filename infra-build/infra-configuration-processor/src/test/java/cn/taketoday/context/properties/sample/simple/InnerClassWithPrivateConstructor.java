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

package cn.taketoday.context.properties.sample.simple;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Nested properties with a private constructor.
 *
 * @author Phillip Webb
 */
@ConfigurationProperties(prefix = "config")
public class InnerClassWithPrivateConstructor {

  private Nested nested = new Nested("whatever");

  public Nested getNested() {
    return this.nested;
  }

  public void setNested(Nested nested) {
    this.nested = nested;
  }

  public static final class Nested {

    private String name;

    private Nested(String ignored) {
    }

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

  }

}
