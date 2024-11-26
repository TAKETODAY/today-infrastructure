/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.core.conversion.support;

import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;

/**
 * Calls {@link Enum#ordinal()} to convert a source Enum to a Integer.
 * This converter will not match enums with interfaces that can be converted.
 *
 * @author Yanming Zhou
 * @since 4.0
 */
final class EnumToIntegerConverter extends AbstractConditionalEnumConverter implements Converter<Enum<?>, Integer> {

  public EnumToIntegerConverter(ConversionService conversionService) {
    super(conversionService);
  }

  @Override
  public Integer convert(Enum<?> source) {
    return source.ordinal();
  }

}
