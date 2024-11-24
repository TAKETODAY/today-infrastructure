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

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.format.annotation.PeriodStyle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PeriodToStringConverter}.
 *
 * @author Eddú Melendez
 * @author Edson Chávez
 */
class PeriodToStringConverterTests {

  @ConversionServiceTest
  void convertWithoutStyleShouldReturnIso8601(ConversionService conversionService) {
    String converted = conversionService.convert(Period.ofDays(1), String.class);
    assertThat(converted).isEqualTo(Period.ofDays(1).toString());
  }

  @ConversionServiceTest
  void convertWithFormatWhenZeroShouldUseFormatAndDays(ConversionService conversionService) {
    String converted = (String) conversionService.convert(Period.ofMonths(0),
            MockPeriodTypeDescriptor.get(null, PeriodStyle.SIMPLE), TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("0d");
  }

  @ConversionServiceTest
  void convertWithFormatShouldUseFormat(ConversionService conversionService) {
    String converted = (String) conversionService.convert(Period.of(1, 2, 3),
            MockPeriodTypeDescriptor.get(null, PeriodStyle.SIMPLE), TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("1y2m3d");
  }

  @ConversionServiceTest
  void convertWithFormatAndUnitWhenZeroShouldUseFormatAndUnit(ConversionService conversionService) {
    String converted = (String) conversionService.convert(Period.ofYears(0),
            MockPeriodTypeDescriptor.get(ChronoUnit.YEARS, PeriodStyle.SIMPLE),
            TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("0y");
  }

  @ConversionServiceTest
  void convertWithFormatAndUnitWhenNonZeroShouldUseFormatAndIgnoreUnit(ConversionService conversionService) {
    String converted = (String) conversionService.convert(Period.of(1, 0, 3),
            MockPeriodTypeDescriptor.get(ChronoUnit.YEARS, PeriodStyle.SIMPLE),
            TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("1y3d");
  }

  @ConversionServiceTest
  void convertWithWeekUnitShouldConvertToStringInDays(ConversionService conversionService) {
    String converted = (String) conversionService.convert(Period.ofWeeks(53),
            MockPeriodTypeDescriptor.get(null, PeriodStyle.SIMPLE), TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("371d");
  }

  static Stream<? extends Arguments> conversionServices() throws Exception {
    return ConversionServiceArguments.with(new PeriodToStringConverter());
  }

}
