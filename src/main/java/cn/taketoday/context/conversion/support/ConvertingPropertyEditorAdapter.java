/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.conversion.support;

import java.beans.PropertyEditorSupport;

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.utils.Assert;

/**
 * Adapter that exposes a {@link java.beans.PropertyEditor} for any given
 * {@link ConversionService} and specific target type.
 *
 * @author Juergen Hoeller
 * @author TODAY
 * @since 3.0
 */
public class ConvertingPropertyEditorAdapter extends PropertyEditorSupport {

  private final Class<?> targetType;
  private final boolean canConvertToString;
  private final ConversionService conversionService;

  /**
   * Create a new ConvertingPropertyEditorAdapter for a given
   * {@link ConversionService} and the given target type.
   *
   * @param conversionService
   *         the ConversionService to delegate to
   * @param targetType
   *         the target type to convert to
   */
  public ConvertingPropertyEditorAdapter(ConversionService conversionService, Class<?> targetType) {
    Assert.notNull(conversionService, "ConversionService must not be null");
    Assert.notNull(targetType, "targetType must not be null");
    this.targetType = targetType;
    this.conversionService = conversionService;
    this.canConvertToString = conversionService.canConvert(targetType, String.class);
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    setValue(this.conversionService.convert(text, targetType));
  }

  @Override
  public String getAsText() {
    if (this.canConvertToString) {
      return this.conversionService.convert(getValue(), String.class);
    }
    else {
      return null;
    }
  }

}
