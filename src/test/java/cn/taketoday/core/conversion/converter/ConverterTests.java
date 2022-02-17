/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.core.conversion.converter;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.conversion.Converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Converter}
 *
 * @author Josh Cummings
 * @author Sam Brannen
 */
class ConverterTests {

  private final Converter<Integer, Integer> moduloTwo = number -> number % 2;
  private final Converter<Integer, Integer> addOne = number -> number + 1;

  @Test
  void andThenWhenGivenANullConverterThenThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.moduloTwo.andThen(null));
  }

  @Test
  void andThenWhenGivenConverterThenComposesInOrder() {
    assertThat(this.moduloTwo.andThen(this.addOne).convert(13)).isEqualTo(2);
    assertThat(this.addOne.andThen(this.moduloTwo).convert(13)).isEqualTo(0);
  }

  @Test
  void andThenCanConvertfromDifferentSourceType() {
    Converter<String, Integer> length = String::length;
    assertThat(length.andThen(this.moduloTwo).convert("example")).isEqualTo(1);
    assertThat(length.andThen(this.addOne).convert("example")).isEqualTo(8);
  }

  @Test
  void andThenCanConvertToDifferentTargetType() {
    Converter<String, Integer> length = String::length;
    Converter<Integer, String> toString = Object::toString;
    assertThat(length.andThen(toString).convert("example")).isEqualTo("7");
    assertThat(toString.andThen(length).convert(1_000)).isEqualTo(4);
  }

}
