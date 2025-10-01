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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import infra.bytecode.Type;

/**
 * @version $Id: ReflectUtils.java,v 1.30 2009/01/11 19:47:49 herbyderby Exp $
 */
@SuppressWarnings({ "rawtypes" })
public abstract class CglibReflectUtils {

  @Nullable
  public static String[] getNames(final Class[] classes) {
    if (classes == null) {
      return null;
    }
    int i = 0;
    final String[] names = new String[classes.length];
    for (final Class clazz : classes) {
      names[i++] = clazz.getName();
    }
    return names;
  }

  public static int findPackageProtected(Class[] classes) {
    for (int i = 0; i < classes.length; i++) {
      if (!Modifier.isPublic(classes[i].getModifiers())) {
        return i;
      }
    }
    return 0;
  }

  // used by MethodInterceptorGenerated generated code
  public static Method[] findMethods(String[] namesAndDescriptors, Method[] methods) {

    final HashMap<String, Method> map = new HashMap<>();
    for (final Method method : methods) {
      map.put(method.getName().concat(Type.getMethodDescriptor(method)), method);
    }

    final Method[] result = new Method[namesAndDescriptors.length / 2];
    for (int i = 0; i < result.length; i++) {
      result[i] = map.get(namesAndDescriptors[i * 2] + namesAndDescriptors[i * 2 + 1]);
    }
    return result;
  }

}
