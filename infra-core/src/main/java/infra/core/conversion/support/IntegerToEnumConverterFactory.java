/*
 * Copyright 2002-present the original author or authors.
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

package infra.core.conversion.support;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalConverter;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.util.Assert;

/**
 * Converts from a Integer to a {@link Enum} by calling {@link Class#getEnumConstants()}.
 *
 * @author Yanming Zhou
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
final class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum>, ConditionalConverter {

  @Override
  public <T extends Enum> Converter<Integer, T> getConverter(Class<T> targetType) {
    Class<?> enumType = ConversionUtils.resolveEnumType(targetType);
    Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
    return new IntegerToEnum(enumType);
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return ConversionUtils.resolveEnumType(targetType.getType()) != null;
  }

  private record IntegerToEnum<T extends Enum>(Class<T> enumType) implements Converter<Integer, T> {

    @Override
    public T convert(Integer source) {
      return this.enumType.getEnumConstants()[source];
    }
  }

}
