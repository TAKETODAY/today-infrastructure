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

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NumberToPeriodConverter}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 */
class NumberToPeriodConverterTests {

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, 10)).hasDays(10);
    assertThat(convert(conversionService, +10)).hasDays(10);
    assertThat(convert(conversionService, -10)).hasDays(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnPeriod(ConversionService conversionService) {
    assertThat(convert(conversionService, 10, ChronoUnit.DAYS)).hasDays(10);
    assertThat(convert(conversionService, -10, ChronoUnit.DAYS)).hasDays(-10);
    assertThat(convert(conversionService, 10, ChronoUnit.WEEKS)).isEqualTo(Period.ofWeeks(10));
    assertThat(convert(conversionService, -10, ChronoUnit.WEEKS)).isEqualTo(Period.ofWeeks(-10));
    assertThat(convert(conversionService, 10, ChronoUnit.MONTHS)).hasMonths(10);
    assertThat(convert(conversionService, -10, ChronoUnit.MONTHS)).hasMonths(-10);
    assertThat(convert(conversionService, 10, ChronoUnit.YEARS)).hasYears(10);
    assertThat(convert(conversionService, -10, ChronoUnit.YEARS)).hasYears(-10);
  }

  private Period convert(ConversionService conversionService, Integer source) {
    return conversionService.convert(source, Period.class);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Period convert(ConversionService conversionService, Integer source, ChronoUnit defaultUnit) {
    TypeDescriptor targetType = mock(TypeDescriptor.class);
    if (defaultUnit != null) {
      PeriodUnit unitAnnotation = AnnotationUtils
              .synthesizeAnnotation(Collections.singletonMap("value", defaultUnit), PeriodUnit.class, null);
      given(targetType.getAnnotation(PeriodUnit.class)).willReturn(unitAnnotation);
    }
    given(targetType.getType()).willReturn((Class) Period.class);
    return (Period) conversionService.convert(source, TypeDescriptor.fromObject(source), targetType);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new NumberToPeriodConverter());
  }

}
