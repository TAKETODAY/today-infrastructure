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

import java.beans.PropertyEditorSupport;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Adapter that exposes a {@link java.beans.PropertyEditor} for any given
 * {@link ConversionService} and specific target type.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ConvertingPropertyEditorAdapter extends PropertyEditorSupport {

  private final ConversionService conversionService;

  private final TypeDescriptor targetDescriptor;

  private final boolean canConvertToString;

  /**
   * Create a new ConvertingPropertyEditorAdapter for a given
   * {@link ConversionService}
   * and the given target type.
   *
   * @param conversionService the ConversionService to delegate to
   * @param targetDescriptor the target type to convert to
   */
  public ConvertingPropertyEditorAdapter(ConversionService conversionService, TypeDescriptor targetDescriptor) {
    Assert.notNull(conversionService, "ConversionService is required");
    Assert.notNull(targetDescriptor, "TypeDescriptor is required");
    this.targetDescriptor = targetDescriptor;
    this.conversionService = conversionService;
    this.canConvertToString = conversionService.canConvert(targetDescriptor, TypeDescriptor.valueOf(String.class));
  }

  @Override
  public void setAsText(@Nullable String text) throws IllegalArgumentException {
    setValue(conversionService.convert(text, TypeDescriptor.valueOf(String.class), targetDescriptor));
  }

  @Override
  @Nullable
  public String getAsText() {
    if (canConvertToString) {
      return (String) conversionService.convert(getValue(), targetDescriptor, TypeDescriptor.valueOf(String.class));
    }
    else {
      return null;
    }
  }

}
