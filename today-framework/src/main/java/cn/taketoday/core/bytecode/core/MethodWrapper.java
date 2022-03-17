/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.core.bytecode.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author TODAY <br>
 * 2019-09-03 12:58
 */
public class MethodWrapper {

  private static final MethodWrapperKey KEY_FACTORY = KeyFactory.create(MethodWrapperKey.class);

  /** Internal interface, only public due to ClassLoader issues. */
  public interface MethodWrapperKey {

    Object newInstance(String name, String[] parameterTypes, String returnType);
  }

  private MethodWrapper() { }

  public static Object create(Method method) {

    return KEY_FACTORY.newInstance(method.getName(),
                                   CglibReflectUtils.getNames(method.getParameterTypes()),
                                   method.getReturnType().getName());
  }

  public static HashSet<Object> createSet(Collection<Method> methods) {
    final HashSet<Object> ret = new HashSet<>();

    for (final Method method : methods) {
      ret.add(create(method));
    }
    return ret;
  }
}
