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

package infra.expression.spel.support;

import java.util.function.Supplier;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionException;
import infra.core.conversion.ConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.expression.TypeConverter;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.lang.Assert;
import infra.lang.Nullable;

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
