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

package cn.taketoday.format.support;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import cn.taketoday.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CharArrayFormatter}.
 *
 * @author Phillip Webb
 */
class CharArrayFormatterTests {

  @ConversionServiceTest
  void convertFromCharArrayToStringShouldConvert(ConversionService conversionService) {
    char[] source = { 'b', 'o', 'o', 't' };
    String converted = conversionService.convert(source, String.class);
    assertThat(converted).isEqualTo("boot");
  }

  @ConversionServiceTest
  void convertFromStringToCharArrayShouldConvert(ConversionService conversionService) {
    String source = "boot";
    char[] converted = conversionService.convert(source, char[].class);
    assertThat(converted).containsExactly('b', 'o', 'o', 't');
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new CharArrayFormatter());
  }

}
