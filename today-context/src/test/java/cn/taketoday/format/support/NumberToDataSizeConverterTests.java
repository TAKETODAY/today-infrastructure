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

import java.util.Collections;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.DataSizeUnit;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.DataUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NumberToDataSizeConverter}.
 *
 * @author Stephane Nicoll
 */
class NumberToDataSizeConverterTests {

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, 10)).isEqualTo(DataSize.ofBytes(10));
    assertThat(convert(conversionService, +10)).isEqualTo(DataSize.ofBytes(10));
    assertThat(convert(conversionService, -10)).isEqualTo(DataSize.ofBytes(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, 10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
    assertThat(convert(conversionService, +10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
    assertThat(convert(conversionService, -10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(-10));
  }

  private DataSize convert(ConversionService conversionService, Integer source) {
    return conversionService.convert(source, DataSize.class);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private DataSize convert(ConversionService conversionService, Integer source, DataUnit defaultUnit) {
    TypeDescriptor targetType = mock(TypeDescriptor.class);
    if (defaultUnit != null) {
      DataSizeUnit unitAnnotation = AnnotationUtils
              .synthesizeAnnotation(Collections.singletonMap("value", defaultUnit), DataSizeUnit.class, null);
      given(targetType.getAnnotation(DataSizeUnit.class)).willReturn(unitAnnotation);
    }
    given(targetType.getType()).willReturn((Class) DataSize.class);
    return (DataSize) conversionService.convert(source, TypeDescriptor.fromObject(source), targetType);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new NumberToDataSizeConverter());
  }

}
