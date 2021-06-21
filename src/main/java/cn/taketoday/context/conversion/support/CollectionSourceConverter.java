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

import java.util.Collection;

import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.GenericDescriptor;

/**
 * @author TODAY 2021/3/22 13:04
 * @since 3.0
 */
public abstract class CollectionSourceConverter implements TypeConverter {

  @Override
  public boolean supports(GenericDescriptor targetType, Class<?> sourceType) {
    return CollectionUtils.isCollection(sourceType) && supportsInternal(targetType, sourceType);
  }

  protected abstract boolean supportsInternal(GenericDescriptor targetType, Class<?> sourceType);

  @Override
  public Object convert(GenericDescriptor targetType, Object source) {
    return convertInternal(targetType, (Collection<?>) source);
  }

  protected abstract Object convertInternal(GenericDescriptor targetType, Collection<?> sourceCollection);
}
