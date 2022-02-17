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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Converts a comma-delimited String to a Collection.
 * If the target collection element type is declared, only matches if
 * {@code String.class} can be converted to it.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY
 * @see StringUtils#splitAsList(String)
 * @since 3.0
 */
final class StringToCollectionConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  public StringToCollectionConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(String.class, Collection.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return (targetType.getElementDescriptor() == null ||
            this.conversionService.canConvert(sourceType, targetType.getElementDescriptor()));
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    String string = (String) source;

    String[] fields = StringUtils.commaDelimitedListToStringArray(string);
    TypeDescriptor elementDesc = targetType.getElementDescriptor();
    Collection<Object> target = CollectionUtils.createCollection(targetType.getType(),
            (elementDesc != null ? elementDesc.getType() : null), fields.length);
    if (elementDesc == null) {
      for (String field : fields) {
        target.add(field.trim());
      }
    }
    else {
      for (String field : fields) {
        Object targetElement = this.conversionService.convert(field.trim(), sourceType, elementDesc);
        target.add(targetElement);
      }
    }
    return target;
  }

}
