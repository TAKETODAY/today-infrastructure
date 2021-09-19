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
package cn.taketoday.cglib.core;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.asm.Attribute;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.core.reflect.ReflectionException;
import cn.taketoday.util.ReflectionUtils;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.STATIC;

/**
 * @version $Id: ReflectUtils.java,v 1.30 2009/01/11 19:47:49 herbyderby Exp $
 */
@SuppressWarnings("all")
public abstract class CglibReflectUtils {

  private static final Method defineClass;

  private static final ArrayList<Method> OBJECT_METHODS;

  private static final String[] CGLIB_PACKAGES = { "java.lang" };

  static {
    try {
      defineClass = ClassLoader.class.getDeclaredMethod(
              "defineClass",
              String.class,
              byte[].class,
              Integer.TYPE,
              Integer.TYPE,
              ProtectionDomain.class
      );
      ReflectionUtils.makeAccessible(defineClass);
    }
    catch (NoSuchMethodException e) {
      throw new CodeGenerationException(e);
    }

    Method[] declaredMethods = Object.class.getDeclaredMethods();
    ArrayList<Method> objectMethods = new ArrayList<>(declaredMethods.length);
    for (Method method : declaredMethods) {
      if ("finalize".equals(method.getName()) || (method.getModifiers() & (FINAL | STATIC)) > 0) {
        continue;
      }
      objectMethods.add(method);
    }
    // @since 4.0
    objectMethods.trimToSize();
    OBJECT_METHODS = objectMethods;
  }

  public static ProtectionDomain getProtectionDomain(final Class<?> source) {
    return source == null ? null : AccessController.doPrivileged((PrivilegedAction<ProtectionDomain>) source::getProtectionDomain);
  }

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
    final Method m = findInterfaceMethod(iface);
    if (m.getName().equals("newInstance")) {
      return m;
    }
    throw new IllegalArgumentException(iface + " missing newInstance method");
  }

  public static Method[] getPropertyMethods(PropertyDescriptor[] properties, boolean read, boolean write) {
    final Set<Method> methods = new HashSet<>();
    for (int i = 0; i < properties.length; i++) {
      PropertyDescriptor pd = properties[i];
      if (read) {
        methods.add(pd.getReadMethod());
      }
      if (write) {
        methods.add(pd.getWriteMethod());
      }
    }
    methods.remove(null);
    return methods.toArray(new Method[methods.size()]);
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

  public static List<Method> addAllMethods(final Class<?> type, final List<Method> list) {

    if (type == Object.class) {
      list.addAll(OBJECT_METHODS);
    }
    else {
      Collections.addAll(list, type.getDeclaredMethods());
    }

    final Class<?> superclass = type.getSuperclass();
    if (superclass != null) {
      addAllMethods(superclass, list);
    }
    for (final Class<?> interface_ : type.getInterfaces()) {
      addAllMethods(interface_, list);
    }
    return list;
  }

  public static List<Class<?>> addAllInterfaces(Class<?> type, List<Class<?>> list) {
    final Class<?> superclass = type.getSuperclass();
    if (superclass != null) {
      Collections.addAll(list, type.getInterfaces());
      addAllInterfaces(superclass, list);
    }
    return list;
  }

  public static Method findInterfaceMethod(Class iface) {
    if (iface.isInterface()) {
      Method found = null;
      for (final Method method : iface.getDeclaredMethods()) {
        if (!method.isDefault()) {
          if (found != null) {
            throw new IllegalArgumentException("expecting exactly 1 method in " + iface);
          }
          found = method;
        }
      }
      return found;
    }
    throw new IllegalArgumentException(iface + " is not an interface");
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> defineClass(
          String className, byte[] bytes, ClassLoader loader, ProtectionDomain protection) throws Exception //
  {
    try {
      return (Class<T>) defineClass.invoke(loader, className, bytes, 0, bytes.length, protection);
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new ReflectionException("defineClass failed", e);
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

  public static MethodInfo getMethodInfo(final Member member, final int modifiers) {
    final MethodSignature sig = MethodSignature.from(member);
    return new MethodInfo() {
      private ClassInfo ci;

      public ClassInfo getClassInfo() {
        if (ci == null)
          ci = CglibReflectUtils.getClassInfo(member.getDeclaringClass());
        return ci;
      }

      public int getModifiers() {
        return modifiers;
      }

      public MethodSignature getSignature() {
        return sig;
      }

      public Type[] getExceptionTypes() {
        return Type.getExceptionTypes(member);
      }

      public Attribute getAttribute() {
        return null;
      }
    };
  }

  public static MethodInfo getMethodInfo(Member member) {
    return getMethodInfo(member, member.getModifiers());
  }

  public static ClassInfo getClassInfo(final Class clazz) {
    final Type type = Type.fromClass(clazz);
    final Type sc = (clazz.getSuperclass() == null) ? null : Type.fromClass(clazz.getSuperclass());
    return new ClassInfo() {
      public Type getType() {
        return type;
      }

      public Type getSuperType() {
        return sc;
      }

      public Type[] getInterfaces() {
        return Type.getTypes(clazz.getInterfaces());
      }

      public int getModifiers() {
        return clazz.getModifiers();
      }
    };
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
