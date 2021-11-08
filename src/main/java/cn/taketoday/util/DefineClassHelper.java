/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

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

package cn.taketoday.util;


import cn.taketoday.core.reflect.ReflectionException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * Helper class for invoking {@link ClassLoader#defineClass(String, byte[], int, int)}.
 *
 * @author TODAY 2021/11/8 9:44
 * @since 4.0
 */
public class DefineClassHelper {

  /**
   * Loads a class file by a given class loader.
   *
   * <p>This first tries to use {@code java.lang.invoke.MethodHandle} to load a class.
   * Otherwise, or if {@code neighbor} is null,
   * this tries to use {@code sun.misc.Unsafe} to load a class.
   * Then it tries to use a {@code protected} method in {@code java.lang.ClassLoader}
   * via {@code PrivilegedAction}.  Since the latter approach is not available
   * any longer by default in Java 9 or later, the JVM argument
   * {@code --add-opens java.base/java.lang=ALL-UNNAMED} must be given to the JVM.
   * If this JVM argument cannot be given, {@link #toPublicClass(String, byte[])}
   * should be used instead.
   * </p>
   *
   * @param className the name of the loaded class.
   * @param neighbor the class contained in the same package as the loaded class.
   * @param loader the class loader.  It can be null if {@code neighbor} is not null
   * and the JVM is Java 11 or later.
   * @param domain if it is null, a default domain is used.
   * @param bcode the bytecode for the loaded class.
   */
  public static Class<?> toClass(String className, Class<?> neighbor, ClassLoader loader,
                                 ProtectionDomain domain, byte[] bcode)
          throws ReflectionException {
    try {
      return Helper.defineClass(className, bcode, 0, bcode.length,
              neighbor, loader, domain);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (ClassFormatError e) {
      Throwable t = e.getCause();
      throw new ReflectionException(t == null ? e : t);
    }
    catch (Exception e) {
      throw new ReflectionException(e);
    }
  }

  /**
   * Loads a class file by {@code java.lang.invoke.MethodHandles.Lookup}.
   * It is obtained by using {@code neighbor}.
   *
   * @param neighbor a class belonging to the same package that the loaded
   * class belogns to.
   * @param bcode the bytecode.
   */
  public static Class<?> toClass(Class<?> neighbor, byte[] bcode)
          throws ReflectionException {
    try {
      DefineClassHelper.class.getModule().addReads(neighbor.getModule());
      Lookup lookup = MethodHandles.lookup();
      Lookup prvlookup = MethodHandles.privateLookupIn(neighbor, lookup);
      return prvlookup.defineClass(bcode);
    }
    catch (IllegalAccessException | IllegalArgumentException e) {
      throw new ReflectionException(e.getMessage() + ": " + neighbor.getName()
              + " has no permission to define the class");
    }
  }

  /**
   * Loads a class file by {@code java.lang.invoke.MethodHandles.Lookup}.
   * It can be obtained by {@code MethodHandles.lookup()} called from
   * somewhere in the package that the loaded class belongs to.
   *
   * @param bcode the bytecode.
   */
  public static Class<?> toClass(Lookup lookup, byte[] bcode)
          throws ReflectionException {
    try {
      return lookup.defineClass(bcode);
    }
    catch (IllegalAccessException | IllegalArgumentException e) {
      throw new ReflectionException(e.getMessage());
    }
  }

  /**
   * Loads a class file by {@code java.lang.invoke.MethodHandles.Lookup}.
   */
  static Class<?> toPublicClass(String className, byte[] bcode) throws ReflectionException {
    try {
      Lookup lookup = MethodHandles.lookup();
      lookup = lookup.dropLookupMode(java.lang.invoke.MethodHandles.Lookup.PRIVATE);
      return lookup.defineClass(bcode);
    }
    catch (Throwable t) {
      throw new ReflectionException(t);
    }
  }

  private static class Helper {
    private static final Method defineClass = getDefineClassMethod();

    private static Method getDefineClassMethod() {
      try {
        return ClassLoader.class.getDeclaredMethod("defineClass",
                String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
      }
      catch (NoSuchMethodException e) {
        throw new RuntimeException("cannot initialize", e);
      }
    }

    static Class<?> defineClass(String name, byte[] b, int off, int len, Class<?> neighbor,
                                ClassLoader loader, ProtectionDomain protectionDomain)
            throws ClassFormatError, ReflectionException //
    {
      if (neighbor != null)
        return toClass(neighbor, b);
      else {
        // Lookup#defineClass() is not available.  So fallback to invoking defineClass on
        // ClassLoader, which causes a warning message.
        try {
          defineClass.setAccessible(true);
          return (Class<?>) defineClass.invoke(loader, new Object[] {
                  name, b, off, len, protectionDomain
          });
        }
        catch (Throwable e) {
          if (e instanceof ClassFormatError)
            throw (ClassFormatError) e;
          if (e instanceof RuntimeException)
            throw (RuntimeException) e;
          throw new ReflectionException(e);
        }
      }
    }
  }
}