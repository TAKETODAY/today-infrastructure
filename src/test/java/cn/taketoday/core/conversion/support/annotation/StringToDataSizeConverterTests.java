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

import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.DataUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link StringToDataSizeConverter}.
 *
 * @author Stephane Nicoll
 */
class StringToDataSizeConverterTests {

  @ConversionServiceTest
  void convertWhenSimpleBytesShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, "10B")).isEqualTo(DataSize.ofBytes(10));
    assertThat(convert(conversionService, "+10B")).isEqualTo(DataSize.ofBytes(10));
    assertThat(convert(conversionService, "-10B")).isEqualTo(DataSize.ofBytes(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleKilobytesShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, "10KB")).isEqualTo(DataSize.ofKilobytes(10));
    assertThat(convert(conversionService, "+10KB")).isEqualTo(DataSize.ofKilobytes(10));
    assertThat(convert(conversionService, "-10KB")).isEqualTo(DataSize.ofKilobytes(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleMegabytesShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, "10MB")).isEqualTo(DataSize.ofMegabytes(10));
    assertThat(convert(conversionService, "+10MB")).isEqualTo(DataSize.ofMegabytes(10));
    assertThat(convert(conversionService, "-10MB")).isEqualTo(DataSize.ofMegabytes(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleGigabytesShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, "10GB")).isEqualTo(DataSize.ofGigabytes(10));
    assertThat(convert(conversionService, "+10GB")).isEqualTo(DataSize.ofGigabytes(10));
    assertThat(convert(conversionService, "-10GB")).isEqualTo(DataSize.ofGigabytes(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleTerabytesShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, "10TB")).isEqualTo(DataSize.ofTerabytes(10));
    assertThat(convert(conversionService, "+10TB")).isEqualTo(DataSize.ofTerabytes(10));
    assertThat(convert(conversionService, "-10TB")).isEqualTo(DataSize.ofTerabytes(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, "10")).isEqualTo(DataSize.ofBytes(10));
    assertThat(convert(conversionService, "+10")).isEqualTo(DataSize.ofBytes(10));
    assertThat(convert(conversionService, "-10")).isEqualTo(DataSize.ofBytes(-10));
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDataSize(ConversionService conversionService) {
    assertThat(convert(conversionService, "10", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
    assertThat(convert(conversionService, "+10", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
    assertThat(convert(conversionService, "-10", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(-10));
  }

  @ConversionServiceTest
  void convertWhenBadFormatShouldThrowException(ConversionService conversionService) {
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> convert(conversionService, "10WB"))
            .withMessageContaining("'10WB' is not a valid data size");
  }

  @ConversionServiceTest
  void convertWhenEmptyShouldReturnNull(ConversionService conversionService) {
    assertThat(convert(conversionService, "")).isNull();
  }

  private DataSize convert(ConversionService conversionService, String source) {
    return conversionService.convert(source, DataSize.class);
  }

  private DataSize convert(ConversionService conversionService, String source, DataUnit unit) {
    return (DataSize) conversionService.convert(source, TypeDescriptor.fromObject(source),
            MockDataSizeTypeDescriptor.get(unit));
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new StringToDataSizeConverter());
  }

}
