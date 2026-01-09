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

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionFailedException;
import infra.core.conversion.ConversionService;
import infra.core.conversion.GenericConverter;
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
