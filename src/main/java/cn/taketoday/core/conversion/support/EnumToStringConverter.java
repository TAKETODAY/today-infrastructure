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

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;

/**
 * Calls {@link Enum#name()} to convert a source Enum to a String.
 * This converter will not match enums with interfaces that can be converted.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
final class EnumToStringConverter extends AbstractConditionalEnumConverter implements Converter<Enum<?>, String> {

  public EnumToStringConverter(ConversionService conversionService) {
    super(conversionService);
  }

  @Override
  public String convert(Enum<?> source) {
    return source.name();
  }

}
