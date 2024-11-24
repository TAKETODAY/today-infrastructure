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

package infra.format.support;

import org.junit.jupiter.params.provider.Arguments;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.format.annotation.DurationFormat;

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
            MockDurationTypeDescriptor.get(null, DurationFormat.Style.SIMPLE), TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("1000ms");
  }

  @ConversionServiceTest
  void convertWithFormatAndUnitShouldUseFormatAndUnit(ConversionService conversionService) {
    String converted = (String) conversionService.convert(Duration.ofSeconds(1),
            MockDurationTypeDescriptor.get(ChronoUnit.SECONDS, DurationFormat.Style.SIMPLE),
            TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("1s");
  }

  static Stream<? extends Arguments> conversionServices() throws Exception {
    return ConversionServiceArguments.with(new DurationToStringConverter());
  }

}
