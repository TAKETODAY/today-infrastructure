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

package infra.context.properties.sample.immutable;

import infra.context.properties.sample.ConstructorBinding;

/**
 * Simple immutable properties with several constructors.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutableMultiConstructorProperties {

  private final String name;

  /**
   * Test description.
   */
  private final String description;

  public ImmutableMultiConstructorProperties(String name) {
    this(name, null);
  }

  @ConstructorBinding
  public ImmutableMultiConstructorProperties(String name, String description) {
    this.name = name;
    this.description = description;
  }

}
