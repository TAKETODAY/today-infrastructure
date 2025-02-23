/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Stream;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.format.support.ApplicationConversionService;
import infra.format.support.ConversionServiceTest;
import infra.format.support.DefaultFormattingConversionService;
import infra.format.support.FormattingConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/23 22:56
 */
class ConfigurationPropertiesCharSequenceToObjectConverterTests {

  @ConversionServiceTest
  void convertWhenCanConvertViaToString(ConversionService conversionService) {
    assertThat(conversionService.convert(new StringBuilder("1"), Integer.class)).isOne();
  }

  @ConversionServiceTest
  void convertWhenCanConvertDirectlySkipsStringConversion(ConversionService conversionService) {
    assertThat(conversionService.convert(new String("1"), Long.class)).isOne();
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
    List<String> converted = (List<String>) conversionService.convert(source, sourceType, targetType);
    assertThat(converted).containsExactly("1", "2", "3");
  }

  @Test
  @SuppressWarnings("unchecked")
  void convertWhenTargetIsListAndNotUsingApplicationConversionService() {
    FormattingConversionService conversionService = new DefaultFormattingConversionService();
    conversionService.addConverter(new ConfigurationPropertiesCharSequenceToObjectConverter(conversionService));
    StringBuilder source = new StringBuilder("1,2,3");
    TypeDescriptor sourceType = TypeDescriptor.valueOf(StringBuilder.class);
    TypeDescriptor targetType = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));
    List<String> converted = (List<String>) conversionService.convert(source, sourceType, targetType);
    assertThat(converted).containsExactly("1", "2", "3");
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with((conversionService) -> {
      conversionService.addConverter(new StringToIntegerConverter());
      conversionService.addConverter(new StringToLongConverter());
      conversionService.addConverter(new CharSequenceToLongConverter());
      conversionService.addConverter(new ConfigurationPropertiesCharSequenceToObjectConverter(conversionService));
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
      return Long.parseLong(source.toString()) + 1;
    }

  }

}