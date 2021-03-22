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

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.utils.ClassUtils;

/**
 * A base implementation for enum-based converters.
 *
 * @author Stephane Nicoll
 * @author TODAY
 * @since 3.0
 */
abstract class AbstractConditionalEnumConverter {
  private final ConversionService conversionService;

  protected AbstractConditionalEnumConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public boolean matches(Class<?> sourceType, Class<?> targetType) {
    for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClassAsSet(sourceType)) {
      if (this.conversionService.canConvert(interfaceType, targetType)) {
        return false;
      }
    }
    return true;
  }

}
