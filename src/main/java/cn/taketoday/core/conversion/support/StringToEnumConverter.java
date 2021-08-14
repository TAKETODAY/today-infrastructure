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

package cn.taketoday.core.conversion.support;

import cn.taketoday.core.conversion.TypeConverter;
import cn.taketoday.core.utils.GenericDescriptor;

/**
 * @author TODAY 2021/3/22 16:45
 * @since 3.0
 */
public class StringToEnumConverter extends StringSourceTypeConverter implements TypeConverter {

  @Override
  public boolean supportsInternal(GenericDescriptor targetClass, Class<?> sourceType) {
    return targetClass.isEnum();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object convertInternal(GenericDescriptor targetClass, String source) {
    if (source.isEmpty()) {
      // It's an empty enum identifier: reset the enum value to null.
      return null;
    }
    return Enum.valueOf((Class<Enum>) targetClass.getType(), source.trim());
  }

}
