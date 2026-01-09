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

import org.jspecify.annotations.Nullable;

import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * Converts from a String to a {@link Enum} by calling {@link Enum#valueOf(Class, String)}.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 * @since 3.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
final class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

  @Override
  public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
    return new StringToEnum(ClassUtils.getEnumType(targetType));
  }

  private record StringToEnum<T extends Enum>(Class<T> enumType) implements Converter<String, T> {

    @Override
    @Nullable
    public T convert(String source) {
      if (StringUtils.isEmpty(source)) {
        // It's an empty enum identifier: reset the enum value to null.
        return null;
      }
      return (T) Enum.valueOf(this.enumType, source.trim());
    }
  }

}
