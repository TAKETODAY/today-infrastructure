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

import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.util.NumberUtils;

/**
 * Converts from a Character to any JDK-standard Number implementation.
 *
 * <p>Support Number classes including Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal. This class
 * delegates to {@link NumberUtils#convertNumberToTargetClass(Number, Class)} to perform the conversion.
 *
 * @author Keith Donald
 * @see Byte
 * @see Short
 * @see Integer
 * @see Long
 * @see java.math.BigInteger
 * @see Float
 * @see Double
 * @see java.math.BigDecimal
 * @see NumberUtils
 * @since 3.0
 */
final class CharacterToNumberFactory implements ConverterFactory<Character, Number> {

  @Override
  public <T extends Number> Converter<Character, T> getConverter(Class<T> targetType) {
    return new CharacterToNumber<>(targetType);
  }

  private record CharacterToNumber<T extends Number>(Class<T> targetType) implements Converter<Character, T> {

    @Override
    public T convert(Character source) {
      return NumberUtils.convertNumberToTargetClass((short) source.charValue(), this.targetType);
    }
  }

}
