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
package cn.taketoday.core.bytecode.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author TODAY <br>
 * 2018-11-08 15:08
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FastConstructorAccessor<T> extends FastMemberAccessor {

  FastConstructorAccessor(MethodAccess fc, Constructor<T> constructor) {
    super(fc, constructor, fc.getIndex(constructor.getParameterTypes()));
  }

  public Class[] getParameterTypes() {
    return ((Constructor<T>) member).getParameterTypes();
  }

  public Class[] getExceptionTypes() {
    return ((Constructor<T>) member).getExceptionTypes();
  }

  public T newInstance() throws InvocationTargetException {
    return (T) fc.newInstance(index, null);
  }

  public T newInstance(Object[] args) throws InvocationTargetException {
    return (T) fc.newInstance(index, args);
  }

  public Constructor<T> getJavaConstructor() {
    return (Constructor<T>) member;
  }
}
