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

package cn.taketoday.core.conversion.support;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Internal utilities for the conversion package.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 * @since 4.0
 */
abstract class ConversionUtils {

  @Nullable
  public static Object invokeConverter(
          GenericConverter converter, @Nullable Object source,
          TypeDescriptor sourceType, TypeDescriptor targetType) {

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

  public static boolean canConvertElements(
          @Nullable TypeDescriptor sourceElementType,
          @Nullable TypeDescriptor targetElementType, ConversionService conversionService) {

    if (targetElementType == null) {
      // yes
      return true;
    }
    if (sourceElementType == null) {
      // maybe
      return true;
    }
    if (conversionService.canConvert(sourceElementType, targetElementType)) {
      // yes
      return true;
    }
    if (ClassUtils.isAssignable(sourceElementType.getType(), targetElementType.getType())) {
      // maybe
      return true;
    }
    // no
    return false;
  }

}
