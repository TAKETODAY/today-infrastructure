/**
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
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
import cn.taketoday.context.Constant;
import cn.taketoday.context.reflect.ReflectionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ReflectionUtils;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.STATIC;

/**
 * @version $Id: ReflectUtils.java,v 1.30 2009/01/11 19:47:49 herbyderby Exp $
 */
@SuppressWarnings("all")
public abstract class CglibReflectUtils {

  private static final HashMap<String, Class> primitives = new HashMap<>();
  private static final HashMap<String, String> transforms = new HashMap<>();

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

  static {
    primitives.put("byte", Byte.TYPE);
    primitives.put("char", Character.TYPE);
    primitives.put("double", Double.TYPE);
    primitives.put("float", Float.TYPE);
    primitives.put("int", Integer.TYPE);
    primitives.put("long", Long.TYPE);
    primitives.put("short", Short.TYPE);
    primitives.put("boolean", Boolean.TYPE);

    transforms.put("byte", "B");
    transforms.put("char", "C");
    transforms.put("double", "D");
    transforms.put("float", "F");
    transforms.put("int", "I");
    transforms.put("long", "J");
    transforms.put("short", "S");
    transforms.put("boolean", "Z");
  }

  public static ProtectionDomain getProtectionDomain(final Class<?> source) {
    return source == null ? null : AccessController.doPrivileged((PrivilegedAction<ProtectionDomain>) source::getProtectionDomain);
  }

  public static Type[] getExceptionTypes(Member member) {
    if (member instanceof Executable) {
      return TypeUtils.getTypes(((Executable) member).getExceptionTypes());
    }
    throw new IllegalArgumentException("Cannot get exception types of a field");
  }

  public static Signature getSignature(Member member) {
    if (member instanceof Method) {
      return new Signature(member.getName(), Type.getMethodDescriptor((Method) member));
    }
    if (member instanceof Constructor) {
      Type[] types = TypeUtils.getTypes(((Constructor) member).getParameterTypes());
      return new Signature(Constant.CONSTRUCTOR_NAME, Type.getMethodDescriptor(Type.VOID_TYPE, types));
    }
    throw new IllegalArgumentException("Cannot get signature of a field");
  }

  public static Constructor findConstructor(String desc) {
    return findConstructor(desc, ClassUtils.getClassLoader());
  }

  public static Constructor findConstructor(String desc, ClassLoader loader) {
    try {
      String className = desc.substring(0, desc.indexOf('(')).trim();
      return getClass(className, loader).getConstructor(parseTypes(desc, loader));
    }
    catch (ClassNotFoundException | NoSuchMethodException e) {
      throw new CodeGenerationException(e);
    }
  }

  public static Method findMethod(String desc) {
    return findMethod(desc, ClassUtils.getClassLoader());
  }

  public static Method findMethod(String desc, ClassLoader loader) {
    try {
      int lparen = desc.indexOf('(');
      int dot = desc.lastIndexOf('.', lparen);
      String className = desc.substring(0, dot).trim();
      String methodName = desc.substring(dot + 1, lparen).trim();
      return getClass(className, loader).getDeclaredMethod(methodName, parseTypes(desc, loader));
    }
    catch (ClassNotFoundException | NoSuchMethodException e) {
      throw new CodeGenerationException(e);
    }
  }

  private static Class[] parseTypes(String desc, ClassLoader loader) throws ClassNotFoundException {
    int lparen = desc.indexOf('(');
    int rparen = desc.indexOf(')', lparen);
    ArrayList<String> params = new ArrayList<>();
    int start = lparen + 1;
    for (; ; ) {
      int comma = desc.indexOf(',', start);
      if (comma < 0) {
        break;
      }
      params.add(desc.substring(start, comma).trim());
      start = comma + 1;
    }
    if (start < rparen) {
      params.add(desc.substring(start, rparen).trim());
    }
    int i = 0;
    Class<?>[] types = new Class[params.size()];
    for (final String name : params) {
      types[i++] = getClass(name, loader);
    }

    return types;
  }

  private static Class getClass(String className, ClassLoader loader) throws ClassNotFoundException {
    return getClass(className, loader, CGLIB_PACKAGES);
  }

  private static Class getClass(String className, ClassLoader loader, String[] packages)
          throws ClassNotFoundException //
  {
    String save = className;
    int dimensions = 0;
    int index = 0;
    while ((index = className.indexOf("[]", index) + 1) > 0) {
      dimensions++;
    }
    StringBuilder brackets = new StringBuilder(className.length() - dimensions);
    for (int i = 0; i < dimensions; i++) {
      brackets.append('[');
    }
    className = className.substring(0, className.length() - 2 * dimensions);

    final String prefix = (dimensions > 0) ? brackets + "L" : Constant.BLANK;
    final String suffix = (dimensions > 0) ? ";" : Constant.BLANK;
    try {
      return Class.forName(new StringBuilder(prefix)
                                   .append(className)
                                   .append(suffix)
                                   .toString(), false, loader);
    }
    catch (ClassNotFoundException ignore) { }
    for (int i = 0; i < packages.length; i++) {
      try {
        return Class.forName(new StringBuilder(prefix)
                                     .append(packages[i])
                                     .append('.')
                                     .append(className)
                                     .append(suffix).toString(), false, loader);
      }
      catch (ClassNotFoundException ignore) { }
    }
    if (dimensions == 0) {
      Class c = primitives.get(className);
      if (c != null) {
        return c;
      }
    }
    else {
      String transform = transforms.get(className);
      if (transform != null) {
        try {
          return Class.forName(brackets.append(transform).toString(), false, loader);
        }
        catch (ClassNotFoundException ignore) { }
      }
    }
    throw new ClassNotFoundException(save);
  }

  public static <T> T newInstance(Class<T> type) {
    return newInstance(type, Constant.EMPTY_CLASS_ARRAY, null);
  }

  public static <T> T newInstance(Class<T> type, Class[] parameterTypes, Object[] args) {
    return newInstance(getConstructor(type, parameterTypes), args);
  }

  public static <T> T newInstance(final Constructor<T> cstruct, final Object[] args) {

    try {
      return cstruct.newInstance(args);
    }
    catch (InstantiationException | IllegalAccessException e) {
      throw new CodeGenerationException(e);
    }
    catch (InvocationTargetException e) {
      throw new CodeGenerationException(e.getTargetException());
    }
  }

  public static <T> Constructor<T> getConstructor(Class<T> type, Class[] parameterTypes) {
    try {
      return ReflectionUtils.accessibleConstructor(type, parameterTypes);
    }
    catch (NoSuchMethodException e) {
      throw new CodeGenerationException(e);
    }
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
      final Method[] methods = iface.getDeclaredMethods();
      if (methods.length != 1) {
        throw new IllegalArgumentException("expecting exactly 1 method in " + iface);
      }
      return methods[0];
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

    final Signature sig = getSignature(member);
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

      public Signature getSignature() {
        return sig;
      }

      public Type[] getExceptionTypes() {
        return CglibReflectUtils.getExceptionTypes(member);
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
        return TypeUtils.getTypes(clazz.getInterfaces());
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
