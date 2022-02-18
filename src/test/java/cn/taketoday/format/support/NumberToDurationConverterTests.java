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
import java.util.Collections;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.DurationUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NumberToDurationConverter}.
 *
 * @author Phillip Webb
 */
class NumberToDurationConverterTests {

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, 10)).hasMillis(10);
    assertThat(convert(conversionService, +10)).hasMillis(10);
    assertThat(convert(conversionService, -10)).hasMillis(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, 10, ChronoUnit.SECONDS)).hasSeconds(10);
    assertThat(convert(conversionService, +10, ChronoUnit.SECONDS)).hasSeconds(10);
    assertThat(convert(conversionService, -10, ChronoUnit.SECONDS)).hasSeconds(-10);
  }

  private Duration convert(ConversionService conversionService, Integer source) {
    return conversionService.convert(source, Duration.class);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Duration convert(ConversionService conversionService, Integer source, ChronoUnit defaultUnit) {
    TypeDescriptor targetType = mock(TypeDescriptor.class);
    if (defaultUnit != null) {
      DurationUnit unitAnnotation = AnnotationUtils
              .synthesizeAnnotation(Collections.singletonMap("value", defaultUnit), DurationUnit.class, null);
      given(targetType.getAnnotation(DurationUnit.class)).willReturn(unitAnnotation);
    }
    given(targetType.getType()).willReturn((Class) Duration.class);
    return (Duration) conversionService.convert(source, TypeDescriptor.fromObject(source), targetType);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new NumberToDurationConverter());
  }

}
