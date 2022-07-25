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

import java.util.Locale;
import java.util.stream.Stream;

import cn.taketoday.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LenientStringToEnumConverterFactory}.
 *
 * @author Phillip Webb
 */
class LenientStringToEnumConverterFactoryTests {

  @ConversionServiceTest
  void canConvertFromStringToEnumShouldReturnTrue(ConversionService conversionService) {
    assertThat(conversionService.canConvert(String.class, TestEnum.class)).isTrue();
  }

  @ConversionServiceTest
  void canConvertFromStringToEnumSubclassShouldReturnTrue(ConversionService conversionService) {
    assertThat(conversionService.canConvert(String.class, TestSubclassEnum.ONE.getClass())).isTrue();
  }

  @ConversionServiceTest
  void convertFromStringToEnumWhenExactMatchShouldConvertValue(ConversionService conversionService) {
    assertThat(conversionService.convert("", TestEnum.class)).isNull();
    assertThat(conversionService.convert("ONE", TestEnum.class)).isEqualTo(TestEnum.ONE);
    assertThat(conversionService.convert("TWO", TestEnum.class)).isEqualTo(TestEnum.TWO);
    assertThat(conversionService.convert("THREE_AND_FOUR", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
  }

  @ConversionServiceTest
  void convertFromStringToEnumWhenFuzzyMatchShouldConvertValue(ConversionService conversionService) {
    assertThat(conversionService.convert("", TestEnum.class)).isNull();
    assertThat(conversionService.convert("one", TestEnum.class)).isEqualTo(TestEnum.ONE);
    assertThat(conversionService.convert("tWo", TestEnum.class)).isEqualTo(TestEnum.TWO);
    assertThat(conversionService.convert("three_and_four", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
    assertThat(conversionService.convert("threeandfour", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
    assertThat(conversionService.convert("three-and-four", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
    assertThat(conversionService.convert("threeAndFour", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
  }

  @ConversionServiceTest
  void convertFromStringToEnumWhenUsingNonEnglishLocaleShouldConvertValue(ConversionService conversionService) {
    Locale defaultLocale = Locale.getDefault();
    try {
      Locale.setDefault(new Locale("tr"));
      LocaleSensitiveEnum result = conversionService.convert("accept-case-insensitive-properties",
              LocaleSensitiveEnum.class);
      assertThat(result).isEqualTo(LocaleSensitiveEnum.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
    }
    finally {
      Locale.setDefault(defaultLocale);
    }
  }

  @ConversionServiceTest
  void convertFromStringToEnumWhenYamlBooleanShouldConvertValue(ConversionService conversionService) {
    assertThat(conversionService.convert("one", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.ONE);
    assertThat(conversionService.convert("two", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.TWO);
    assertThat(conversionService.convert("true", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.ON);
    assertThat(conversionService.convert("false", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.OFF);
    assertThat(conversionService.convert("TRUE", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.ON);
    assertThat(conversionService.convert("FALSE", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.OFF);
    assertThat(conversionService.convert("fA_lsE", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.OFF);
    assertThat(conversionService.convert("one", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.ONE);
    assertThat(conversionService.convert("two", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.TWO);
    assertThat(conversionService.convert("true", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.TRUE);
    assertThat(conversionService.convert("false", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.FALSE);
    assertThat(conversionService.convert("TRUE", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.TRUE);
    assertThat(conversionService.convert("FALSE", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.FALSE);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments
            .with((service) -> service.addConverterFactory(new LenientStringToEnumConverterFactory()));
  }

  enum TestEnum {

    ONE, TWO, THREE_AND_FOUR

  }

  enum TestOnOffEnum {

    ONE, TWO, ON, OFF

  }

  enum TestTrueFalseEnum {

    ONE, TWO, TRUE, FALSE, ON, OFF

  }

  enum LocaleSensitiveEnum {

    ACCEPT_CASE_INSENSITIVE_PROPERTIES

  }

  enum TestSubclassEnum {

    ONE {
      @Override
      public String toString() {
        return "foo";
      }

    }

  }

}
