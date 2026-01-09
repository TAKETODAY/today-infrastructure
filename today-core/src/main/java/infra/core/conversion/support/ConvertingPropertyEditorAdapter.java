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

import java.beans.PropertyEditorSupport;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.lang.Assert;

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
