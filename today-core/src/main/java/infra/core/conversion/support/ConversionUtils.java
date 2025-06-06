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
import infra.core.conversion.ConversionFailedException;
import infra.core.conversion.ConversionService;
import infra.core.conversion.GenericConverter;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * Internal utilities for the conversion package.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class ConversionUtils {

  @Nullable
  public static Object invokeConverter(GenericConverter converter,
          @Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

    try {
      return converter.convert(source, sourceType, targetType);
    }
    catch (ConversionFailedException ex) {
      throw ex;
    }
    catch (Throwable ex) {
      throw new ConversionFailedException(sourceType, targetType, source, ex);
    }
  }

  public static boolean canConvertElements(@Nullable TypeDescriptor sourceElementType,
          @Nullable TypeDescriptor targetElementType, ConversionService conversionService) {

    if (targetElementType == null) {
      // yes
      return true;
    }
    if (sourceElementType == null) {
      // maybe
      return true;
    }
    return conversionService.canConvert(sourceElementType, targetElementType)
            || ClassUtils.isAssignable(sourceElementType.getType(), targetElementType.getType());
  }

}
