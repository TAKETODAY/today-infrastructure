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

package cn.taketoday.expression.spel.support;

import java.util.function.Supplier;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Default implementation of the {@link TypeConverter} interface,
 * delegating to a core {@link ConversionService}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.core.conversion.ConversionService
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
    Assert.notNull(conversionService, "ConversionService must not be null");
    this.conversionService = () -> conversionService;
  }

  /**
   * Create a StandardTypeConverter for the given ConversionService.
   *
   * @param conversionService a Supplier for the ConversionService to delegate to
   */
  public StandardTypeConverter(Supplier<ConversionService> conversionService) {
    Assert.notNull(conversionService, "Supplier must not be null");
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
