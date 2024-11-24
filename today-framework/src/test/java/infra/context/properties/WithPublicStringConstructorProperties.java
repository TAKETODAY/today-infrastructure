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

package infra.context.properties;

import infra.context.properties.ConfigurationProperties;

/**
 * A {@link ConfigurationProperties @ConfigurationProperties} with an additional
 * single-arg public constructor. Used in {@link ConfigurationPropertiesTests}.
 *
 * @author Madhura Bhave
 */
@ConfigurationProperties(prefix = "test")
public class WithPublicStringConstructorProperties {

  private String a;

  public WithPublicStringConstructorProperties() {
  }

  public WithPublicStringConstructorProperties(String a) {
    this.a = a;
  }

  public String getA() {
    return this.a;
  }

  public void setA(String a) {
    this.a = a;
  }

}
