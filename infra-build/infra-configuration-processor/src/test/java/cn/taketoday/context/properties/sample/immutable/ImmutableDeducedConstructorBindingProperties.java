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

package cn.taketoday.context.properties.sample.immutable;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import cn.taketoday.context.properties.sample.DefaultValue;

/**
 * @author Madhura Bhave
 */
@ConfigurationProperties("immutable")
public class ImmutableDeducedConstructorBindingProperties {

  /**
   * The name of these properties.
   */
  private final String theName;

  /**
   * A simple flag.
   */
  private final boolean flag;

  public ImmutableDeducedConstructorBindingProperties(@DefaultValue("boot") String theName, boolean flag) {
    this.theName = theName;
    this.flag = flag;
  }

  public String getTheName() {
    return this.theName;
  }

  public boolean isFlag() {
    return this.flag;
  }

}
