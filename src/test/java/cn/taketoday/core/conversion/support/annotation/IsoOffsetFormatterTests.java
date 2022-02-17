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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import cn.taketoday.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IsoOffsetFormatter}.
 *
 * @author Phillip Webb
 */
class IsoOffsetFormatterTests {

  @ConversionServiceTest
  void convertShouldConvertStringToIsoDate(ConversionService conversionService) {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime converted = conversionService.convert(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now),
            OffsetDateTime.class);
    assertThat(converted).isEqualTo(now);
  }

  @ConversionServiceTest
  void convertShouldConvertIsoDateToString(ConversionService conversionService) {
    OffsetDateTime now = OffsetDateTime.now();
    String converted = conversionService.convert(now, String.class);
    assertThat(converted).isNotNull().startsWith(now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new IsoOffsetFormatter());
  }

}
