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

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.PeriodStyle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToPeriodConverter}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 */
class StringToPeriodConverterTests {

  @ConversionServiceTest
  void convertWhenIso8601ShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, "P2Y")).isEqualTo(Period.parse("P2Y"));
    assertThat(convert(conversionService, "P3M")).isEqualTo(Period.parse("P3M"));
    assertThat(convert(conversionService, "P4W")).isEqualTo(Period.parse("P4W"));
    assertThat(convert(conversionService, "P5D")).isEqualTo(Period.parse("P5D"));
    assertThat(convert(conversionService, "P1Y2M3D")).isEqualTo(Period.parse("P1Y2M3D"));
    assertThat(convert(conversionService, "P1Y2M3W4D")).isEqualTo(Period.parse("P1Y2M3W4D"));
    assertThat(convert(conversionService, "P-1Y2M")).isEqualTo(Period.parse("P-1Y2M"));
    assertThat(convert(conversionService, "-P1Y2M")).isEqualTo(Period.parse("-P1Y2M"));
  }

  @ConversionServiceTest
  void convertWhenSimpleDaysShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, "10d")).isEqualTo(Period.ofDays(10));
    assertThat(convert(conversionService, "10D")).isEqualTo(Period.ofDays(10));
    assertThat(convert(conversionService, "+10d")).isEqualTo(Period.ofDays(10));
    assertThat(convert(conversionService, "-10D")).isEqualTo(Period.ofDays(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleWeeksShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, "10w")).isEqualTo(Period.ofWeeks(10));
    assertThat(convert(conversionService, "10W")).isEqualTo(Period.ofWeeks(10));
    assertThat(convert(conversionService, "+10w")).isEqualTo(Period.ofWeeks(10));
    assertThat(convert(conversionService, "-10W")).isEqualTo(Period.ofWeeks(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleMonthsShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, "10m")).isEqualTo(Period.ofMonths(10));
    assertThat(convert(conversionService, "10M")).isEqualTo(Period.ofMonths(10));
    assertThat(convert(conversionService, "+10m")).isEqualTo(Period.ofMonths(10));
    assertThat(convert(conversionService, "-10M")).isEqualTo(Period.ofMonths(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleYearsShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, "10y")).isEqualTo(Period.ofYears(10));
    assertThat(convert(conversionService, "10Y")).isEqualTo(Period.ofYears(10));
    assertThat(convert(conversionService, "+10y")).isEqualTo(Period.ofYears(10));
    assertThat(convert(conversionService, "-10Y")).isEqualTo(Period.ofYears(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, "10")).isEqualTo(Period.ofDays(10));
    assertThat(convert(conversionService, "+10")).isEqualTo(Period.ofDays(10));
    assertThat(convert(conversionService, "-10")).isEqualTo(Period.ofDays(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, "10", ChronoUnit.DAYS, null)).isEqualTo(Period.ofDays(10));
    assertThat(convert(conversionService, "+10", ChronoUnit.DAYS, null)).isEqualTo(Period.ofDays(10));
    assertThat(convert(conversionService, "-10", ChronoUnit.DAYS, null)).isEqualTo(Period.ofDays(-10));
    assertThat(convert(conversionService, "10", ChronoUnit.WEEKS, null)).isEqualTo(Period.ofWeeks(10));
    assertThat(convert(conversionService, "+10", ChronoUnit.WEEKS, null)).isEqualTo(Period.ofWeeks(10));
    assertThat(convert(conversionService, "-10", ChronoUnit.WEEKS, null)).isEqualTo(Period.ofWeeks(-10));
    assertThat(convert(conversionService, "10", ChronoUnit.MONTHS, null)).isEqualTo(Period.ofMonths(10));
    assertThat(convert(conversionService, "+10", ChronoUnit.MONTHS, null)).isEqualTo(Period.ofMonths(10));
    assertThat(convert(conversionService, "-10", ChronoUnit.MONTHS, null)).isEqualTo(Period.ofMonths(-10));
    assertThat(convert(conversionService, "10", ChronoUnit.YEARS, null)).isEqualTo(Period.ofYears(10));
    assertThat(convert(conversionService, "+10", ChronoUnit.YEARS, null)).isEqualTo(Period.ofYears(10));
    assertThat(convert(conversionService, "-10", ChronoUnit.YEARS, null)).isEqualTo(Period.ofYears(-10));
  }

  private Period convert(ConversionService conversionService, String source) {
    return conversionService.convert(source, Period.class);
  }

  private Period convert(ConversionService conversionService, String source, ChronoUnit unit, PeriodStyle style) {
    return (Period) conversionService.convert(source, TypeDescriptor.fromObject(source),
            MockPeriodTypeDescriptor.get(unit, style));
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new StringToPeriodConverter());
  }

}
