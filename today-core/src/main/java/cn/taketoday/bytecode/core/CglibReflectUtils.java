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
package cn.taketoday.bytecode.core;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import cn.taketoday.bytecode.Type;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

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

  public static Class[] getClasses(final Object[] objects) {
    int i = 0;
    final Class[] classes = new Class[objects.length];
    for (final Object obj : objects) {
      classes[i++] = obj.getClass();
    }
    return classes;
  }

  public static Method findNewInstance(Class<?> iface) {
    final Method m = ReflectionUtils.findFunctionalInterfaceMethod(iface);
    if (m.getName().equals("newInstance")) {
      return m;
    }
    throw new IllegalArgumentException(iface + " missing newInstance method");
  }

  public static Method[] getPropertyMethods(PropertyDescriptor[] properties, boolean read, boolean write) {
    final HashSet<Method> methods = new HashSet<>();
    for (PropertyDescriptor pd : properties) {
      if (read) {
        methods.add(pd.getReadMethod());
      }
      if (write) {
        methods.add(pd.getWriteMethod());
      }
    }
    methods.remove(null);
    return ReflectionUtils.toMethodArray(methods);
  }

  public static PropertyDescriptor[] getBeanProperties(Class<?> type) {
    return getPropertiesHelper(type, true, true);
  }

  public static PropertyDescriptor[] getBeanGetters(Class<?> type) {
    return getPropertiesHelper(type, true, false);
  }

  public static PropertyDescriptor[] getBeanSetters(Class<?> type) {
    return getPropertiesHelper(type, false, true);
  }

  private static PropertyDescriptor[] getPropertiesHelper(Class<?> type, boolean read, boolean write) {
    try {
      PropertyDescriptor[] all = Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors();
      if (read && write) {
        return all;
      }
      final ArrayList<PropertyDescriptor> properties = new ArrayList<>(all.length);
      for (final PropertyDescriptor pd : all) {
        if ((read && pd.getReadMethod() != null) || (write && pd.getWriteMethod() != null)) {
          properties.add(pd);
        }
      }
      return properties.toArray(new PropertyDescriptor[properties.size()]);
    }
    catch (IntrospectionException e) {
      throw new CodeGenerationException(e);
    }
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
