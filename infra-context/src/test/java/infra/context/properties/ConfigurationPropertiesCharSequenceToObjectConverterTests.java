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