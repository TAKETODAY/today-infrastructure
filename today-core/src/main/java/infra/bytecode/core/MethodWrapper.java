/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.bytecode.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author TODAY
 * @since 2019-09-03 12:58
 */
public final class MethodWrapper {

  private MethodWrapper() {

  }

  public static Object create(Method method) {
    return new MethodWrapperKey(method.getName(),
            Arrays.asList(CglibReflectUtils.getNames(method.getParameterTypes())),
            method.getReturnType().getName());
  }

  public static HashSet<Object> createSet(Collection<Method> methods) {
    HashSet<Object> ret = new HashSet<>();

    for (Method method : methods) {
      ret.add(create(method));
    }
    return ret;
  }

  private record MethodWrapperKey(String name, List<String> parameterTypes, String returnType) {
  }

}
