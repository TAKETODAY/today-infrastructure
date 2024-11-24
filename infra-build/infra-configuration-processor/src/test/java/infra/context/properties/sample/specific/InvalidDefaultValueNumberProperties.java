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

package infra.context.properties.sample.specific;

import infra.context.properties.sample.ConfigurationProperties;
import infra.context.properties.sample.DefaultValue;

/**
 * Demonstrates that an invalid default number value leads to a compilation failure.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("test")
public class InvalidDefaultValueNumberProperties {

  private final int counter;

  public InvalidDefaultValueNumberProperties(@DefaultValue("invalid") int counter) {
    this.counter = counter;
  }

  public int getCounter() {
    return this.counter;
  }

}
