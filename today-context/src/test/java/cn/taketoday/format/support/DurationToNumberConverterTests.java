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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.DurationStyle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DurationToNumberConverter}.
 *
 * @author Phillip Webb
 */
class DurationToNumberConverterTests {

  @ConversionServiceTest
  void convertWithoutStyleShouldReturnMs(ConversionService conversionService) {
    Long converted = conversionService.convert(Duration.ofSeconds(1), Long.class);
    assertThat(converted).isEqualTo(1000);
  }

  @ConversionServiceTest
  void convertWithFormatShouldUseIgnoreFormat(ConversionService conversionService) {
    Integer converted = (Integer) conversionService.convert(Duration.ofSeconds(1),
            MockDurationTypeDescriptor.get(null, DurationStyle.ISO8601), TypeDescriptor.valueOf(Integer.class));
    assertThat(converted).isEqualTo(1000);
  }

  @ConversionServiceTest
  void convertWithFormatAndUnitShouldUseFormatAndUnit(ConversionService conversionService) {
    Byte converted = (Byte) conversionService.convert(Duration.ofSeconds(1),
            MockDurationTypeDescriptor.get(ChronoUnit.SECONDS, null), TypeDescriptor.valueOf(Byte.class));
    assertThat(converted).isEqualTo((byte) 1);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new DurationToNumberConverter());
  }

}
