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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Consumer;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.lang.Nullable;

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
            Arguments.of(new NamedConversionService(new ApplicationConversionService(),
                    "Application conversion service")));
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
    public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return null;
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

    @Override
    public <T> T convert(@Nullable Object source, TypeDescriptor targetType) {
      return null;
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
