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
 * Simple immutable properties with primitive wrapper types and defaults.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutablePrimitiveWrapperWithDefaultsProperties {

  private final Boolean flag;

  private final Byte octet;

  private final Character letter;

  private final Short number;

  private final Integer counter;

  private final Long value;

  private final Float percentage;

  private final Double ratio;

  public ImmutablePrimitiveWrapperWithDefaultsProperties(@DefaultValue("true") Boolean flag,
          @DefaultValue("120") Byte octet, @DefaultValue("a") Character letter, @DefaultValue("1000") Short number,
          @DefaultValue("42") Integer counter, @DefaultValue("2000") Long value,
          @DefaultValue("0.5") Float percentage, @DefaultValue("42.42") Double ratio) {
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
