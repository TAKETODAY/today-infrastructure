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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Consumer;
import java.util.stream.Stream;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.core.conversion.GenericConverter;
import infra.format.Formatter;

/**
 * Factory for creating a {@link Stream stream} of {@link Arguments} for use in a
 * {@link ParameterizedTest parameterized test}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class ConversionServiceArguments {

  private ConversionServiceArguments() {
  }

  static Stream<? extends Arguments> with(Formatter<?> formatter) {
    return with((conversionService) -> conversionService.addFormatter(formatter));
  }

  static Stream<? extends Arguments> with(GenericConverter converter) {
    return with((conversionService) -> conversionService.addConverter(converter));
  }

  static Stream<? extends Arguments> with(Consumer<FormattingConversionService> initializer) {
    FormattingConversionService withoutDefaults = new FormattingConversionService();
    initializer.accept(withoutDefaults);
    return Stream.of(
            Arguments.of(new NamedConversionService(withoutDefaults, "Without defaults conversion service")),
            Arguments.of(new NamedConversionService(
                    new ApplicationConversionService(), "Application conversion service"))
    );
  }

  static boolean isApplicationConversionService(ConversionService conversionService) {
    if (conversionService instanceof NamedConversionService) {
      return isApplicationConversionService(((NamedConversionService) conversionService).delegate);
    }
    return conversionService instanceof ApplicationConversionService;
  }

  static class NamedConversionService implements ConversionService {

    private final ConversionService delegate;

    private final String name;

    NamedConversionService(ConversionService delegate, String name) {
      this.delegate = delegate;
      this.name = name;
    }

    @Override
    @Nullable
    public GenericConverter getConverter(Class<?> sourceType, TypeDescriptor targetType) {
      return delegate.getConverter(sourceType, targetType);
    }

    @Override
    @Nullable
    public GenericConverter getConverter(Object sourceObject, TypeDescriptor targetType) {
      return delegate.getConverter(sourceObject, targetType);
    }

    @Override
    @Nullable
    public GenericConverter getConverter(Object sourceObject, Class<?> targetType) {
      return delegate.getConverter(sourceObject, targetType);
    }

    @Override
    @Nullable
    public GenericConverter getConverter(Class<?> sourceType, Class<?> targetType) {
      return delegate.getConverter(sourceType, targetType);
    }

    @Nullable
    @Override
    public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return delegate.getConverter(sourceType, targetType);
    }

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
      return this.delegate.canConvert(sourceType, targetType);
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return this.delegate.canConvert(sourceType, targetType);
    }

    @Override
    public <T> T convert(Object source, Class<T> targetType) {
      return this.delegate.convert(source, targetType);
    }

    @Nullable
    @Override
    public <T> T convert(@Nullable Object source, TypeDescriptor targetType) {
      return delegate.convert(source, targetType);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return this.delegate.convert(source, sourceType, targetType);
    }

    @Override
    public String toString() {
      return this.name;
    }

  }

}
