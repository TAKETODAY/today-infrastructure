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

import java.util.Set;

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterFactory;
import cn.taketoday.core.conversion.ConverterRegistry;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.lang.Nullable;

/**
 * A factory for common {@link cn.taketoday.core.conversion.ConversionService}
 * configurations.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 */
public final class ConversionServiceFactory {

  private ConversionServiceFactory() { }

  /**
   * Register the given Converter objects with the given target ConverterRegistry.
   *
   * @param converters the converter objects: implementing {@link Converter},
   * {@link ConverterFactory}, or {@link GenericConverter}
   * @param registry the target registry
   */
  public static void registerConverters(@Nullable Set<?> converters, ConverterRegistry registry) {
    if (converters != null) {
      for (Object converter : converters) {
        if (converter instanceof GenericConverter) {
          registry.addConverter((GenericConverter) converter);
        }
        else if (converter instanceof Converter<?, ?>) {
          registry.addConverter((Converter<?, ?>) converter);
        }
        else if (converter instanceof ConverterFactory<?, ?>) {
          registry.addConverterFactory((ConverterFactory<?, ?>) converter);
        }
        else {
          throw new IllegalArgumentException("Each converter object must implement one of the " +
                  "Converter, ConverterFactory, or GenericConverter interfaces");
        }
      }
    }
  }

}
