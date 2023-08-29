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

package cn.taketoday.context.properties.sample.specific;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import cn.taketoday.context.properties.sample.ConstructorBinding;
import cn.taketoday.context.properties.sample.DefaultValue;

/**
 * Demonstrates that an empty default value on a property leads to no default value.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("test")
public class EmptyDefaultValueProperties {

  private final String name;

  @ConstructorBinding
  public EmptyDefaultValueProperties(@DefaultValue String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

}
