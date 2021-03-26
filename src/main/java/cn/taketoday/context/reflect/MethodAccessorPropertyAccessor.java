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

package cn.taketoday.context.reflect;

import java.lang.reflect.Method;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * @author TODAY 2020/9/11 15:54
 */
public class MethodAccessorPropertyAccessor
        extends SetterSupport implements PropertyAccessor {

  private final MethodAccessor writeAccessor;
  private final MethodAccessor readAccessor;

  public MethodAccessorPropertyAccessor(boolean primitive, Method setMethod, Method getMethod) {
    super(primitive);
    Assert.notNull(setMethod, "setMethod must not be null");
    Assert.notNull(getMethod, "getMethod must not be null");

    this.writeAccessor = ReflectionUtils.newMethodAccessor(setMethod);
    this.readAccessor = ReflectionUtils.newMethodAccessor(getMethod);
  }

  public MethodAccessorPropertyAccessor(
          boolean primitive, MethodAccessor writeAccessor, MethodAccessor readAccessor) {
    super(primitive);
    Assert.notNull(writeAccessor, "writeAccessor must not be null");
    Assert.notNull(readAccessor, "readAccessor must not be null");
    this.readAccessor = readAccessor;
    this.writeAccessor = writeAccessor;
  }

  @Override
  public Object get(final Object obj) {
    return readAccessor.invoke(obj, null);
  }

  @Override
  protected void setInternal(Object obj, Object value) {
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
