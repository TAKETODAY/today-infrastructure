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

package cn.taketoday.core.conversion.support.annotation;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.util.DataSize;

/**
 * {@link Converter} to convert from a {@link Number} to a {@link DataSize}.
 *
 * @author Stephane Nicoll
 * @see DataSizeUnit
 */
final class NumberToDataSizeConverter implements GenericConverter {

  private final StringToDataSizeConverter delegate = new StringToDataSizeConverter();

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Number.class, DataSize.class));
  }

  @Override
  public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    return this.delegate.convert((source != null) ? source.toString() : null, TypeDescriptor.valueOf(String.class),
            targetType);
  }

}
