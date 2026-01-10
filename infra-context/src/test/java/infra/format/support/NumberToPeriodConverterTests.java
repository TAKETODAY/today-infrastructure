/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.format.support;

import org.junit.jupiter.params.provider.Arguments;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.stream.Stream;

import infra.core.TypeDescriptor;
import infra.core.annotation.AnnotationUtils;
import infra.core.conversion.ConversionService;
import infra.format.annotation.PeriodUnit;

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
    return (Period) conversionService.convert(source, TypeDescriptor.forObject(source), targetType);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new NumberToPeriodConverter());
  }

}
