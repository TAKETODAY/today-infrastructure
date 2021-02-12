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

package cn.taketoday.context.conversion;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.utils.OrderUtils;

/**
 * @author TODAY
 * 2021/1/8 22:41
 */
public class GenericConverter implements Converter, Ordered {
  final Class<?> sourceClass;
  final Converter converter;

  public GenericConverter(Class<?> sourceClass, Converter converter) {
    this.sourceClass = sourceClass;
    this.converter = converter;
  }

  public boolean supports(Object source) {
    return sourceClass.isInstance(source);
  }

  @Override
  public Object convert(Object source) {
    return converter.convert(source);
  }

  // order support
  @Override
  public int getOrder() {
    return OrderUtils.getOrder(converter);
  }
}
