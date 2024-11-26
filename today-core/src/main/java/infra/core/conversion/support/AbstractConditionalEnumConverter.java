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

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalConverter;
import infra.core.conversion.ConversionService;
import infra.util.ClassUtils;

/**
 * A {@link ConditionalConverter} base implementation for enum-based converters.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
abstract class AbstractConditionalEnumConverter implements ConditionalConverter {

  private final ConversionService conversionService;

  protected AbstractConditionalEnumConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClassAsSet(sourceType.getType())) {
      if (this.conversionService.canConvert(TypeDescriptor.valueOf(interfaceType), targetType)) {
        return false;
      }
    }
    return true;
  }

}
