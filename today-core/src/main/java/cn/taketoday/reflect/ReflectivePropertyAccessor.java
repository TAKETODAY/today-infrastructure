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

package cn.taketoday.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * java reflect {@link Field} implementation
 *
 * @author TODAY 2020/9/11 17:56
 */
final class ReflectivePropertyAccessor extends PropertyAccessor {

  @Nullable
  private final Field field;

  private final Method readMethod;
  private final Method writeMethod;

  ReflectivePropertyAccessor(@Nullable Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    this.field = field;
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
  }

  @Override
  public Object get(final Object obj) {
    if (field != null) {
      return ReflectionUtils.getField(field, obj);
    }
    return ReflectionUtils.invokeMethod(readMethod, obj);
  }

  @Override
  public void set(Object obj, Object value) {
    if (field != null) {
      ReflectionUtils.setField(field, obj, value);
    }
    else {
      ReflectionUtils.invokeMethod(writeMethod, obj, value);
    }
  }

  @Override
  public Method getReadMethod() {
    return readMethod;
  }

  @Override
  public Method getWriteMethod() {
    return writeMethod;
  }
}
