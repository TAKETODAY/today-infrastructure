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

package cn.taketoday.core.conversion.support.annotation;

import org.junit.jupiter.params.provider.Arguments;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DurationToStringConverter}.
 *
 * @author Phillip Webb
 */
class DurationToStringConverterTests {

  @ConversionServiceTest
  void convertWithoutStyleShouldReturnIso8601(ConversionService conversionService) {
    String converted = conversionService.convert(Duration.ofSeconds(1), String.class);
    assertThat(converted).isEqualTo("PT1S");
  }

  @ConversionServiceTest
  void convertWithFormatShouldUseFormatAndMs(ConversionService conversionService) {
    String converted = (String) conversionService.convert(Duration.ofSeconds(1),
            MockDurationTypeDescriptor.get(null, DurationStyle.SIMPLE), TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("1000ms");
  }

  @ConversionServiceTest
  void convertWithFormatAndUnitShouldUseFormatAndUnit(ConversionService conversionService) {
    String converted = (String) conversionService.convert(Duration.ofSeconds(1),
            MockDurationTypeDescriptor.get(ChronoUnit.SECONDS, DurationStyle.SIMPLE),
            TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("1s");
  }

  static Stream<? extends Arguments> conversionServices() throws Exception {
    return ConversionServiceArguments.with(new DurationToStringConverter());
  }

}
