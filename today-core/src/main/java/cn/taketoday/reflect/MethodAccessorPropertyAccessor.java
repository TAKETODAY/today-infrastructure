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

import java.lang.reflect.Method;

/**
 * MethodInvoker PropertyAccessor implementation
 *
 * @author TODAY 2020/9/11 15:54
 */
final class MethodAccessorPropertyAccessor extends PropertyAccessor {
  private final MethodInvoker readAccessor;
  private final MethodInvoker writeAccessor;

  MethodAccessorPropertyAccessor(MethodInvoker readAccessor, MethodInvoker writeAccessor) {
    this.readAccessor = readAccessor;
    this.writeAccessor = writeAccessor;
  }

  @Override
  public Object get(final Object obj) {
    return readAccessor.invoke(obj, null);
  }

  @Override
  public void set(Object obj, Object value) {
    writeAccessor.invoke(obj, new Object[] { value });
  }

  @Override
  public Method getReadMethod() {
    return readAccessor.getMethod();
  }

  @Override
  public Method getWriteMethod() {
    return writeAccessor.getMethod();
  }
}
