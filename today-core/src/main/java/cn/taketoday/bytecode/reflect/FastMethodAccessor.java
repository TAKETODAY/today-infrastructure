/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.bytecode.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings({ "rawtypes" })
public class FastMethodAccessor extends FastMemberAccessor {

  FastMethodAccessor(MethodAccess fc, Method method) {
    super(fc, method, helper(fc, method));
  }

  private static int helper(MethodAccess fc, Method method) {
    int index = fc.getIndex(method);
    if (index < 0) {
      Class[] types = method.getParameterTypes();
      System.err.println("hash=" + method.getName().hashCode() + " size=" + types.length);
      for (int i = 0; i < types.length; i++) {
        System.err.println("  types[" + i + "]=" + types[i].getName());
      }
      throw new IllegalArgumentException("Cannot find method " + method);
    }
    return index;
  }

  public Class getReturnType() {
    return ((Method) member).getReturnType();
  }

  @Override
  public Class[] getParameterTypes() {
    return ((Method) member).getParameterTypes();
  }

  @Override
  public Class[] getExceptionTypes() {
    return ((Method) member).getExceptionTypes();
  }

  public Object invoke(Object obj, Object[] args) throws InvocationTargetException {
    return fc.invoke(index, obj, args);
  }

}
