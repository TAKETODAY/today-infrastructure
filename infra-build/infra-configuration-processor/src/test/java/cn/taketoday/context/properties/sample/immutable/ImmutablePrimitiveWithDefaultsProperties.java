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

import cn.taketoday.context.properties.sample.DefaultValue;

/**
 * Simple immutable properties with primitive types and defaults.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutablePrimitiveWithDefaultsProperties {

  private final boolean flag;

  private final byte octet;

  private final char letter;

  private final short number;

  private final int counter;

  private final long value;

  private final float percentage;

  private final double ratio;

  public ImmutablePrimitiveWithDefaultsProperties(@DefaultValue("true") boolean flag, @DefaultValue("120") byte octet,
          @DefaultValue("a") char letter, @DefaultValue("1000") short number, @DefaultValue("42") int counter,
          @DefaultValue("2000") long value, @DefaultValue("0.5") float percentage,
          @DefaultValue("42.42") double ratio) {
    this.flag = flag;
    this.octet = octet;
    this.letter = letter;
    this.number = number;
    this.counter = counter;
    this.value = value;
    this.percentage = percentage;
    this.ratio = ratio;
  }

}
