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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.format.support.FormattingConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CharSequenceToObjectConverter}
 *
 * @author Phillip Webb
 */
class CharSequenceToObjectConverterTests {

  @ConversionServiceTest
  void convertWhenCanConvertViaToString(ConversionService conversionService) {
    assertThat(conversionService.convert(new StringBuilder("1"), Integer.class)).isEqualTo(1);
  }

  @ConversionServiceTest
  void convertWhenCanConvertDirectlySkipsStringConversion(ConversionService conversionService) {
    assertThat(conversionService.convert(new String("1"), Long.class)).isEqualTo(1);
    if (!ConversionServiceArguments.isApplicationConversionService(conversionService)) {
      assertThat(conversionService.convert(new StringBuilder("1"), Long.class)).isEqualTo(2);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void convertWhenTargetIsList() {
    ConversionService conversionService = new ApplicationConversionService();
    StringBuilder source = new StringBuilder("1,2,3");
    TypeDescriptor sourceType = TypeDescriptor.valueOf(StringBuilder.class);
    TypeDescriptor targetType = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));
    List<String> conveted = (List<String>) conversionService.convert(source, sourceType, targetType);
    assertThat(conveted).containsExactly("1", "2", "3");
  }

  @Test
  @SuppressWarnings("unchecked")
  void convertWhenTargetIsListAndNotUsingApplicationConversionService() {
    FormattingConversionService conversionService = new DefaultFormattingConversionService();
    conversionService.addConverter(new CharSequenceToObjectConverter(conversionService));
    StringBuilder source = new StringBuilder("1,2,3");
    TypeDescriptor sourceType = TypeDescriptor.valueOf(StringBuilder.class);
    TypeDescriptor targetType = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));
    List<String> conveted = (List<String>) conversionService.convert(source, sourceType, targetType);
    assertThat(conveted).containsExactly("1", "2", "3");
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with((conversionService) -> {
      conversionService.addConverter(new StringToIntegerConverter());
      conversionService.addConverter(new StringToLongConverter());
      conversionService.addConverter(new CharSequenceToLongConverter());
      conversionService.addConverter(new CharSequenceToObjectConverter(conversionService));
    });
  }

  static class StringToIntegerConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
      return Integer.valueOf(source);
    }

  }

  static class StringToLongConverter implements Converter<String, Long> {

    @Override
    public Long convert(String source) {
      return Long.valueOf(source);
    }

  }

  static class CharSequenceToLongConverter implements Converter<CharSequence, Long> {

    @Override
    public Long convert(CharSequence source) {
      return Long.valueOf(source.toString()) + 1;
    }

  }

}
