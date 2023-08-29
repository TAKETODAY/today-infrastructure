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

/**
 * Simple immutable properties with primitive types.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutablePrimitiveProperties {

  private final boolean flag;

  private final byte octet;

  private final char letter;

  private final short number;

  private final int counter;

  private final long value;

  private final float percentage;

  private final double ratio;

  public ImmutablePrimitiveProperties(boolean flag, byte octet, char letter, short number, int counter, long value,
          float percentage, double ratio) {
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
