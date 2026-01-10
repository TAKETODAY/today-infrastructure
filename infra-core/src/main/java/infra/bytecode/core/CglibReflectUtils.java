/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

  public static String @Nullable [] getNames(final Class @Nullable [] classes) {
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
