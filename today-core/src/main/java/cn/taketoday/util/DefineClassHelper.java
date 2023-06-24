/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import cn.taketoday.bytecode.core.CodeGenerationException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.ReflectionException;

/**
 * Helper class for invoking {@link ClassLoader#defineClass(String, byte[], int, int)}.
 *
 * <p>This first tries to use {@code java.lang.invoke.MethodHandle} to load a class.
 * Otherwise, or if {@code neighbor} is null,
 * this tries to use {@code sun.misc.Unsafe} to load a class.
 * Then it tries to use a {@code protected} method in {@code java.lang.ClassLoader}
 * via {@code PrivilegedAction}.  Since the latter approach is not available
 * any longer by default in Java 9 or later, the JVM argument
 * {@code --add-opens java.base/java.lang=ALL-UNNAMED} must be given to the JVM.
 * </p>
 * <pre>
 * --add-opens java.base/java.lang=ALL-UNNAMED
 * --add-opens java.base/java.util=ALL-UNNAMED
 * --add-opens java.base/java.lang.reflect=ALL-UNNAMED
 * </pre>
 *
 * @author TODAY 2021/11/8 9:44
 * @since 4.0
 */
public class DefineClassHelper {

  @Nullable
  private static final Method classLoaderDefineClassMethod;

  @Nullable
  private static final Throwable THROWABLE;

  @Nullable
  private static final ProtectionDomain PROTECTION_DOMAIN;

  @Nullable
  private static Consumer<Class<?>> loadedClassHandler;

  @Nullable
  private static BiConsumer<String, byte[]> generatedClassHandler;

  static {
    // Resolve protected ClassLoader.defineClass method for fallback use
    // (even if JDK 9+ Lookup.defineClass is preferably used below)
    Method classLoaderDefineClass;
    Throwable throwable = null;
    try {
      classLoaderDefineClass = ClassLoader.class.getDeclaredMethod(
              "defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
      ReflectionUtils.makeAccessible(classLoaderDefineClass);
    }
    catch (Throwable t) {
      classLoaderDefineClass = null;
      throwable = t;
    }

    THROWABLE = throwable;
    classLoaderDefineClassMethod = classLoaderDefineClass;
    PROTECTION_DOMAIN = ReflectionUtils.getProtectionDomain(DefineClassHelper.class);
  }

  public static void setGeneratedClassHandler(@Nullable BiConsumer<String, byte[]> handler) {
    generatedClassHandler = handler;
  }

  public static void setLoadedClassHandler(@Nullable Consumer<Class<?>> handler) {
    loadedClassHandler = handler;
  }

  /**
   * Loads a class file by {@code java.lang.invoke.MethodHandles.Lookup}.
   * It is obtained by using {@code neighbor}.
   *
   * @param neighbor a class belonging to the same package that the loaded
   * class belogns to.
   * @param bcode the bytecode.
   */
  public static Class<?> defineClass(Class<?> neighbor, byte[] bcode)
          throws ReflectionException {
    try {
      DefineClassHelper.class.getModule().addReads(neighbor.getModule());
      Lookup prvlookup = MethodHandles.privateLookupIn(neighbor, MethodHandles.lookup());
      return prvlookup.defineClass(bcode);
    }
    catch (IllegalAccessException | IllegalArgumentException e) {
      throw new ReflectionException(
              e.getMessage() + ": " + neighbor.getName() + " has no permission to define the class", e);
    }
  }

  public static Class<?> defineClass(String className, byte[] b, ClassLoader loader) throws CodeGenerationException {
    return defineClass(className, null, loader, null, b);
  }

  public static Class<?> defineClass(
          String className, byte[] b, ClassLoader loader,
          ProtectionDomain protectionDomain) throws CodeGenerationException {
    return defineClass(className, null, loader, protectionDomain, b);
  }

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
   * </p>
   * <pre>
   * --add-opens java.base/java.lang=ALL-UNNAMED
   * --add-opens java.base/java.util=ALL-UNNAMED
   * --add-opens java.base/java.lang.reflect=ALL-UNNAMED
   * </pre>
   *
   * @param className the name of the loaded class.
   * @param neighbor the class contained in the same package as the loaded class.
   * @param loader the class loader.  It can be null if {@code neighbor} is not null
   * and the JVM is Java 11 or later.
   * @param domain if it is null, a default domain is used.
   * @param classFile the bytecode for the loaded class.
   */
  public static Class<?> defineClass(String className, @Nullable Class<?> neighbor,
          ClassLoader loader, @Nullable ProtectionDomain domain, byte[] classFile) throws CodeGenerationException {

    Class<?> c = null;
    Throwable t = THROWABLE;

    BiConsumer<String, byte[]> handlerToUse = generatedClassHandler;
    if (handlerToUse != null) {
      handlerToUse.accept(className, classFile);
    }
    // Preferred option: JDK 9+ Lookup.defineClass API if ClassLoader matches
    if (neighbor != null && neighbor.getClassLoader() == loader) {
      try {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(neighbor, MethodHandles.lookup());
        c = lookup.defineClass(classFile);
      }
      catch (LinkageError | IllegalArgumentException ex) {
        // in case of plain LinkageError (class already defined)
        // or IllegalArgumentException (class in different package):
        // fall through to traditional ClassLoader.defineClass below
        t = ex;
      }
      catch (Throwable ex) {
        throw newException(className, ex);
      }
    }

    // Direct defineClass attempt on the target Classloader
    if (c == null) {
      if (domain == null) {
        domain = PROTECTION_DOMAIN;
      }

      // Classic option: protected ClassLoader.defineClass method
      if (classLoaderDefineClassMethod != null) {
        try {
          c = (Class<?>) classLoaderDefineClassMethod.invoke(loader, new Object[] { className, classFile, 0, classFile.length, domain });
        }
        catch (InvocationTargetException ex) {
          throw new CodeGenerationException(ex.getTargetException());
        }
        catch (InaccessibleObjectException ex) {
          // Fall through if setAccessible fails with InaccessibleObjectException on JDK 9+
          // (on the module path and/or with a JVM bootstrapped with --illegal-access=deny)
          t = ex;
        }
        catch (Throwable ex) {
          throw newException(className, ex);
        }
      }
      if (c == null) {
        // Look for publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain)
        try {
          Method publicDefineClass = loader.getClass().getMethod(
                  "publicDefineClass", String.class, byte[].class, ProtectionDomain.class);
          c = (Class<?>) publicDefineClass.invoke(loader, className, classFile, domain);
        }
        catch (InvocationTargetException ex) {
          if (!(ex.getTargetException() instanceof UnsupportedOperationException)) {
            throw new CodeGenerationException(ex.getTargetException());
          }
          // in case of UnsupportedOperationException, fall through
          t = ex.getTargetException();
        }
        catch (Throwable ex) {
          // publicDefineClass method not available -> fall through
          t = ex;
        }
      }
    }

    // Fallback option: JDK 9+ Lookup.defineClass API even if ClassLoader does not match
    if (c == null && neighbor != null && neighbor.getClassLoader() != loader) {
      try {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(neighbor, MethodHandles.lookup());
        c = lookup.defineClass(classFile);
      }
      catch (IllegalAccessException ex) {
        throw new CodeGenerationException("ClassLoader mismatch for [" + neighbor.getName() +
                "]: JVM should be started with --add-opens=java.base/java.lang=ALL-UNNAMED " +
                "for ClassLoader.defineClass to be accessible on " + loader.getClass().getName(), ex);
      }
      catch (Throwable ex) {
        throw newException(className, ex);
      }
    }

    // No defineClass variant available at all?
    if (c == null) {
      throw newException(className, t);
    }

    // Force static initializers to run.
//    try {
//      Class.forName(className, true, loader);
//    }
//    catch (ClassNotFoundException e) {
//      throw newException(className, e);
//    }
    return c;
  }

  public static Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
    // Force static initializers to run.
    Class<?> clazz = Class.forName(className, false, classLoader);
    Consumer<Class<?>> handlerToUse = loadedClassHandler;
    if (handlerToUse != null) {
      handlerToUse.accept(clazz);
    }
    return clazz;
  }

  private static CodeGenerationException newException(String className, Throwable ex) {
    return new CodeGenerationException("Class: '" + className + "' define failed", ex);
  }

}
