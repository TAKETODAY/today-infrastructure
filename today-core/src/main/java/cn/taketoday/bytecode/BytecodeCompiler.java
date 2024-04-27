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

package cn.taketoday.bytecode;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import cn.taketoday.bytecode.core.CodeGenerationException;
import cn.taketoday.core.SmartClassLoader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.ReflectionUtils;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/29 11:23
 */
public abstract class BytecodeCompiler {
  private static final int CLASSES_DEFINED_LIMIT = TodayStrategies.getInt("bytecode.classes.defined.limit", 100);

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
    PROTECTION_DOMAIN = ReflectionUtils.getProtectionDomain(BytecodeCompiler.class);
  }

  public static void setGeneratedClassHandler(@Nullable BiConsumer<String, byte[]> handler) {
    generatedClassHandler = handler;
  }

  public static void setLoadedClassHandler(@Nullable Consumer<Class<?>> handler) {
    loadedClassHandler = handler;
  }

  // The child ClassLoader used to load the compiled classes
  protected volatile ChildClassLoader childClassLoader;

  protected BytecodeCompiler(@Nullable ClassLoader classloader) {
    this.childClassLoader = new ChildClassLoader(classloader);
  }

  /**
   * Load a class. Makes sure the classloaders aren't used too much
   * because they anchor compiled classes in memory and prevent GC. If you have expressions
   * continually recompiling over time then by replacing the classloader periodically
   * at least some of the older variants can be garbage collected.
   *
   * @param name the name of the class
   * @param classFile the bytecode for the class
   * @return the Class object for the compiled expression
   */
  @SuppressWarnings("unchecked")
  protected final <T> Class<T> loadClass(String name, byte[] classFile) {
    ChildClassLoader ccl = this.childClassLoader;
    if (ccl.getClassesDefinedCount() >= CLASSES_DEFINED_LIMIT) {
      synchronized(this) {
        ChildClassLoader currentCcl = this.childClassLoader;
        if (ccl == currentCcl) {
          // Still the same ClassLoader that needs to be replaced...
          ccl = new ChildClassLoader(ccl.getParent());
          this.childClassLoader = ccl;
        }
        else {
          // Already replaced by some other thread, let's pick it up.
          ccl = currentCcl;
        }
      }
    }
    return (Class<T>) ccl.defineClass(name, classFile);
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
  @SuppressWarnings("unchecked")
  public static <T> Class<T> compile(String className, @Nullable Class<?> neighbor,
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

      // Look for publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain)
      if (loader instanceof SmartClassLoader smartLoader) {
        try {
          c = smartLoader.publicDefineClass(className, classFile, domain);
        }
        catch (Throwable ex) {
          // publicDefineClass method not available -> fall through
          t = ex;
        }
      }
    }

    // Classic option: protected ClassLoader.defineClass method
    if (c == null && classLoaderDefineClassMethod != null) {
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

    // Fallback option: JDK 9+ Lookup.defineClass API even if ClassLoader does not match
    if (c == null && neighbor != null && neighbor.getClassLoader() != loader) {
      try {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(neighbor, MethodHandles.lookup());
        c = lookup.defineClass(classFile);
      }
      catch (LinkageError | IllegalAccessException ex) {
        throw new CodeGenerationException("ClassLoader mismatch for [" + neighbor.getName() +
                "]: JVM should be started with --add-opens=java.base/java.lang=ALL-UNNAMED " +
                "for ClassLoader.defineClass to be accessible on " + loader.getClass().getName() +
                "; consider co-locating the affected class in that target ClassLoader instead.", ex);
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
    try {
      Class.forName(className, true, loader);
    }
    catch (ClassNotFoundException e) {
      throw newException(className, e);
    }
    return (Class<T>) c;
  }

  public static Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
    // Force static initializers to run.
    Class<?> clazz = Class.forName(className, true, classLoader);
    Consumer<Class<?>> handlerToUse = loadedClassHandler;
    if (handlerToUse != null) {
      handlerToUse.accept(clazz);
    }
    return clazz;
  }

  private static CodeGenerationException newException(String className, Throwable ex) {
    return new CodeGenerationException("Class: '" + className + "' define failed", ex);
  }

  /**
   * A ChildClassLoader will load the generated compiled classes.
   */
  private static class ChildClassLoader extends URLClassLoader {
    private static final URL[] NO_URLS = new URL[0];

    private final AtomicInteger classesDefinedCount = new AtomicInteger(0);

    public ChildClassLoader(@Nullable ClassLoader classLoader) {
      super("bytecode", NO_URLS, classLoader);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
      Class<?> clazz = super.defineClass(name, bytes, 0, bytes.length);
      this.classesDefinedCount.incrementAndGet();
      return clazz;
    }

    public int getClassesDefinedCount() {
      return this.classesDefinedCount.get();
    }

  }

}
