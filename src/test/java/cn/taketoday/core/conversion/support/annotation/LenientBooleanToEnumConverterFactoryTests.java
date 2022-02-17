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

import cn.taketoday.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LenientBooleanToEnumConverterFactory}.
 *
 * @author Madhura Bhave
 */
class LenientBooleanToEnumConverterFactoryTests {

  @ConversionServiceTest
  void convertFromBooleanToEnumWhenShouldConvertValue(ConversionService conversionService) {
    assertThat(conversionService.convert(true, TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.ON);
    assertThat(conversionService.convert(false, TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.OFF);
    assertThat(conversionService.convert(true, TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.TRUE);
    assertThat(conversionService.convert(false, TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.FALSE);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments
            .with((service) -> service.addConverterFactory(new LenientBooleanToEnumConverterFactory()));
  }

  enum TestOnOffEnum {

    ON, OFF

  }

  enum TestTrueFalseEnum {

    ONE, TWO, TRUE, FALSE, ON, OFF

  }

}
