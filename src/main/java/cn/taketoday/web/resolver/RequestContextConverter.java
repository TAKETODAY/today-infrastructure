/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.web.resolver;

import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 *         2019-07-09 23:56
 */
public abstract class RequestContextConverter implements TypeConverter {

  @Override
  public boolean supports(Class<?> targetClass, Object source) {
    return source instanceof RequestContext && supports(targetClass);
  }

  protected boolean supports(Class<?> targetClass) {
    return true;
  }

  @Override
  public final Object convert(Class<?> targetClass, Object source) throws ConversionException {
    return convertInternal(targetClass, (RequestContext) source);
  }

  protected abstract Object convertInternal(Class<?> targetClass, RequestContext source) throws ConversionException;
}
