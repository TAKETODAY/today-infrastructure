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

package infra.expression.spel.support;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionException;
import infra.core.conversion.ConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.expression.TypeConverter;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.lang.Assert;

/**
 * Default implementation of the {@link TypeConverter} interface,
 * delegating to a core {@link ConversionService}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConversionService
 * @since 4.0
 */
public class StandardTypeConverter implements TypeConverter {

  private final Supplier<ConversionService> conversionService;

  /**
   * Create a StandardTypeConverter for the default ConversionService.
   *
   * @see DefaultConversionService#getSharedInstance()
   */
  public StandardTypeConverter() {
    this.conversionService = DefaultConversionService::getSharedInstance;
  }

  /**
   * Create a StandardTypeConverter for the given ConversionService.
   *
   * @param conversionService the ConversionService to delegate to
   */
  public StandardTypeConverter(ConversionService conversionService) {
    Assert.notNull(conversionService, "ConversionService is required");
    this.conversionService = () -> conversionService;
  }

  /**
   * Create a StandardTypeConverter for the given ConversionService.
   *
   * @param conversionService a Supplier for the ConversionService to delegate to
   */
  public StandardTypeConverter(Supplier<ConversionService> conversionService) {
    Assert.notNull(conversionService, "Supplier is required");
    this.conversionService = conversionService;
  }

  @Override
  public boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
    return this.conversionService.get().canConvert(sourceType, targetType);
  }

  @Override
  @Nullable
  public Object convertValue(@Nullable Object value,
          @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
    try {
      return this.conversionService.get().convert(value, sourceType, targetType);
    }
    catch (ConversionException ex) {
      throw new SpelEvaluationException(ex, SpelMessage.TYPE_CONVERSION_ERROR,
              (sourceType != null ? sourceType.toString() : (value != null ? value.getClass().getName() : "null")),
              targetType.toString());
    }
  }

}
